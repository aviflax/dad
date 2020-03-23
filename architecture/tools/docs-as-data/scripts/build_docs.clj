#!/bin/sh
#_(
   #_DEPS is same format as deps.edn. Multiline is okay.
   DEPS='
   {:paths ["tools/docs-as-data/scripts"]
    :deps {selmer                  {:mvn/version "1.12.18"}
           clj-commons/clj-yaml    {:mvn/version "0.7.0"}
           medley                  {:mvn/version "1.2.0"}}}
   '

   #_You can put other options here
   OPTS='-J-Xms256m -J-Xmx256m -J-client'

exec clojure $OPTS -Sdeps "$DEPS" "$0" boom
#_exec clj $OPTS -Sdeps "$DEPS"
)

;; RUN THIS FROM <project-root>/architecture/

(ns build-docs
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.string :as str :refer [lower-case]]
            [medley.core :refer [filter-vals find-first map-kv map-vals]]
            [selmer.filters :as filters]
            [selmer.parser :as parser :refer [render]])
  (:import [java.io FileNotFoundException]))

(defn flexi-get
  ([coll k]
   (flexi-get coll k nil))
  ([coll k d]
   (or (get coll k)
       (get coll (name k))
       (get coll (lower-case (name k)))
       (get coll (keyword (lower-case (name k))))
       d)))

(defn flex=
  [& vs]
  (apply = (map (comp lower-case name) vs)))

(def filters
  ;; TODO: some of these names are iffy. Rethink!
  {:filter-by (fn [m k v]
                (->> m
                     (filter-vals (fn [im] (flex= (flexi-get im k) v)))
                     (into (empty m))))
   :get flexi-get
   ;; We should probably replace this sketchy impl with one from a third-party library
   :slugify #(some-> (name %)
                     (lower-case)
                     (str/replace #"\s" "-"))

   ; This name isn’t clear. It’s really sort-by-nested-key as it assumes that the input is a coll
   ; of maps. Gotta figure this out.
   :sort-by-key (fn [coll k]
                  (into (empty coll)
                        (sort-by (fn [[_k v]] (flexi-get v k))
                                 coll)))
   :sort-by-keys #(into (empty %)
                        (sort-by (fn [[k _v]] (lower-case (name k))) %))
   :sort-by-names (fn [coll]
                    (sort-by (fn [[k v]]
                               (get v :full-name (name k)))
                             coll))
   :without (fn [coll k]
              (remove (fn [[_name props]]
                        (or (contains? props k)
                            (contains? props (keyword k))))
                      coll))})

(defn register-filters!
  []
  (doseq [[n f] filters]
    (filters/add-filter! n f)))

(defn yaml-files
  "Accepts a directory as a path string or a java.io.File, returns a lazy sequence of java.io.File
  objects for all the YAML files in that dir or in any of its child dirs (recursively) to an
  unlimited depth. If the supplied path does not exist or is not a directory, throws."
  [dir]
  (as-> dir v
    (io/file v)
    (cond (not (.exists v)) (throw (FileNotFoundException. (str v " does not exist")))
          (not (.isDirectory v)) (throw (RuntimeException. (str v " is not a directory")))
          :else v)
    (file-seq v)
    (filter #(and (.isFile %)
                (re-seq #".ya?ml$" (.getName %)))
            v)))

(defn file-path->key
  "Accepts a relative file path like data/model/us.yaml and returns :model/us"
  [path]
  (-> path
      (str/replace #"^data/" "")
      (str/replace #".ya?ml$" "")
      keyword))

(defn load-data
  [path]
  (let [files (yaml-files path)
        names (map file-path->key files)]
    (->> (map slurp files)
         (zipmap names)
         (map-vals yaml/parse-string))))

(defn reduce-whitespace
  "Selmer doesn’t have any whitespace control features, so we use this function in post-processing
  to remove some extraneous whitespace. If/when the solution described here:
  https://github.com/yogthos/Selmer/issues/170#issuecomment-594778666 is addressed, we can reassess
  this function."
  [s]
  (-> ;; Selmer rendering tends to yield a ton of extra whitespace, which can cause problems when
      ;; rendering Markdown. So we collapse down two or more blank lines followed stretches of
      ;; whitespace.
      (str/replace s #"\n\s*\n +" "\n\n")
      ;; Collapse 2 or more blank lines into a single blank line.
      (str/replace #"\n{3,}" "\n\n")))

(def warning-header
  (apply str
    (interpose "\n"
      ["<!--"
       ""
       "WARNING WARNING WARNING"
       ""
       "This file is AUTOMATICALLY generated from a template. If you change its contents, your changes will"
       "be lost when the file is regenerated — so DON’T DO THAT."
       ""
       "SERIOUSLY."
       ""
       "Source template: <repo-root>/architecture/{{ _.template.path }}"
       ""
       "-->\n\n"])))

(defn build-docs
  "Returns a map of template-path to rendered template-value. The template paths in the keys are
  relative to the template-path. In other words, they don’t contain the template-path itself."
  [data templates-path]
  (->> (file-seq (io/file templates-path))
       (filter (memfn isFile))
       (map #(vector % (slurp %)))
       (into {})
       (map-vals #(str warning-header %))
       (map-kv (fn [path s]
                 [path (render s (assoc-in data [:_ :template :path] path))]))
       (map-vals reduce-whitespace)))

(defn template-path->output-path
  [template-path templates-path]
  (str/replace template-path (re-pattern (str "^" templates-path "/*")) ""))

(defn write-docs!
  [rendered-docs out-path templates-path]
  (doseq [[template-path doc] rendered-docs
          :let [op (template-path->output-path template-path templates-path)
                out (io/file out-path op)]]
    (io/make-parents out)
    (spit out doc)))

(defn boom
  [data-path templates-path out-path]
  (register-filters!)
  (-> (load-data data-path)
      (build-docs templates-path)
      (write-docs! out-path templates-path)))

(when (= (first *command-line-args*) "boom")
  (boom "data" "document-templates" "documents"))
