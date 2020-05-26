(ns dad.yaml
  (:refer-clojure :exclude [file-seq])
  (:require [clojure.java.io :as io]
            [clj-yaml.core :as yaml])
  (:import [java.io File FileNotFoundException]))

(set! *warn-on-reflection* true)

(defn file-seq
  "Accepts a directory as a path string or a java.io.File, returns a lazy sequence of java.io.File
  objects for all the YAML files in that dir or in any of its child dirs (recursively) to an
  unlimited depth. If the supplied path does not exist or is not a directory, throws."
  [dir]
  (as-> dir v
    (io/file v)
    (cond (not (.exists v)) (throw (FileNotFoundException. (str v " does not exist")))
          (not (.isDirectory v)) (throw (RuntimeException. (str v " is not a directory")))
          :else v)
    (clojure.core/file-seq v)
    (filter (fn [^File fp] (and (.isFile fp)
                                (re-seq #".ya?ml$" (.getName fp))))
            v)))

(def parse-string yaml/parse-string)

(defn stringify
  [v]
  (yaml/generate-string v :dumper-options {:flow-style :block}))
