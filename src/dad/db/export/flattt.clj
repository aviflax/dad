(ns dad.db.export.flattt
  "flattt = “Flatten To Tables” — I’m thinking of extracting this as a standalone library/tool.
  
  Prior art: https://github.com/OpenDataServices/flatten-tool"
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.walk :as walk :refer [keywordize-keys postwalk]]
            [inflections.core :refer [singular]]))

(s/def ::non-blank-string (s/and string? (complement str/blank?)))

(defn scalar?
  [v]
  (or (number? v) (string? v) (keyword? v)))

(s/def ::scalar scalar?)

(s/def ::non-empty-scalar (s/or :number number?
                                :keyword keyword?
                                :string ::non-blank-string))
(s/def ::col-name         keyword?)
(s/def ::col-val          (s/or :scalar      ::non-empty-scalar
                                :scalar-coll (s/coll-of ::non-empty-scalar :gen-max 10)))
(s/def ::record-key-cols  (s/map-of ::col-name ::col-val :gen-max 10))
(s/def ::record-val-cols  (s/map-of ::col-name ::col-val :gen-max 10))
(s/def ::keyed-rows       (s/map-of ::record-key-cols ::record-val-cols :gen-max 10))
(s/def ::unkeyed-rows     (s/coll-of ::record-val-cols :gen-max 10))
(s/def ::table-name       keyword?)
(s/def ::tables           (s/map-of ::table-name
                                    (s/or :keyed ::keyed-rows
                                          :unkeyed ::unkeyed-rows)
                                    :gen-max 10))

(defn- unkeyword
  [v]
  (if (keyword? v)
    (name v)
    v))

(def ^:private separator "-")

(defn- join-names
  [names]
  (try
    (->> (map unkeyword names)
         (str/join separator)
         (keyword))
    (catch Exception e
      (println names)
      (throw e))))

;; This is used only for testing.
(s/def ::simple-test-map
  ;; Using every-kv and every here because they don’t conform, and conformance is both unneeded here
  ;; and also causes problems in the fdef:fn.
  (s/every-kv (s/or :s string? :k keyword?)
              (s/or :s ::non-empty-scalar
                    :sc (s/every ::non-empty-scalar :gen-max 5)
                    :scm (s/every ::simple-test-map :gen-max 2)
                    :m ::simple-test-map)
              :gen-max 5
              :conform-keys true))

(s/fdef fold-props
  :args (s/cat :m (s/with-gen ::simple-test-map
                    #(gen/frequency [[9 (s/gen ::simple-test-map)]
                                     [1 (gen/return {:foo {:bar "baz"
                                                           :props (gen/generate (s/gen ::simple-test-map))}})]])))
  :ret  ::simple-test-map
  :fn   (fn [{{in :m} :args
              out     :ret}]
          (let [map-seq (fn map-seq [m] (tree-seq map? #(interleave (keys %) (vals %)) m))]
            (if (some :props (map-seq in))
              ; TODO: this is incomplete; it doesn’t assert that the entries of :props are retained
              (not-any? :props (map-seq out))
              (= out in)))))

(defn- fold-props
  [m]
  (postwalk
    (fn [v]
      (if-let [props (and (map? v) (map? (:props v)) (:props v))]
        (merge props (dissoc v :props))
        v))
    m))

(defn- pathize
  {:derived-from "https://andersmurphy.com/2019/11/30/clojure-flattening-key-paths.html"}
  ([m]
   (pathize m []))
  ([m path]
   ; (println "map:" m "\npath:" path "\n\n")
   (->> m
       (map (fn [[k v]]
                 ; (println "key:" k "\n" "val:" v "\n\n\n")
                 (cond
                   (and (map? v) (seq v))  (pathize v (conj path k))
                   ; (and (not (map? v))
                   ;      (coll? v)
                   ;      (map? (first v)))  (->> (map-indexed (fn [i v] (pathize v (conj path (join-names [k i])))) v)
                   ;                              (apply concat))
                   :else                   [(conj path k) v])))
        (into {}))))

(defn- interpolate-paths
  [paths]
  (reduce-kv
    (fn [paths path _v]
      (let [path-variants (map #(take % path) (range 2 (count path) 2))]
         (->> (mapv #(vector % (last %)) path-variants)
              (into {})
              (merge paths))))
    paths
    paths))

(defn- key-col-name
  [key-val]
  (if (or (number? key-val)
          (uuid? key-val))
      :id
      :name))

(defn- path+val->tables
  [tables [path v]]
  (let [kpp (partition 2 path)
        val-rows? (and (sequential? v) (map? (first v))) ; is the value a coll of rows?
        table-name-parts (take-nth 2 (if val-rows? path (butlast path)))
        table-name (join-names table-name-parts)
        fk-table? (pos? (count table-name-parts))
        f-keys (if fk-table?
                  (->> (if val-rows? kpp (butlast kpp))
                       (map (fn [[table-name key-val]] [(singular table-name) (unkeyword key-val)]))
                       (into {}))
                  {})
        p-keys (merge f-keys (let [[_ key-val] (last kpp)]
                               {(key-col-name key-val) (unkeyword key-val)}))
        col-name  (if (odd? (count path))
                    (last path)
                    :val)
        col (if (= (last path) v)
              {}
              {col-name v})]
    (if val-rows?
      (update tables table-name concat (map #(merge f-keys %) v))
      (update-in tables [table-name p-keys] merge col))))

(defn db->tables
  [m]
  (->> m
       (keywordize-keys)
       (fold-props)
       (pathize)
       (interpolate-paths)
       (reduce path+val->tables {})))
