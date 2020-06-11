(ns dad.db.export
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [dad.db.export :as e]
            [inflections.core :refer [singular]]
            [medley.core :as mc :refer [map-keys map-vals]]))

(s/def ::non-blank-string (s/and string? (complement str/blank?)))
(s/def ::non-empty-scalar (s/and (complement coll?) (complement empty?)))
(s/def ::col-name (s/or :keyword keyword?
                        :string  ::non-blank-string))

(s/def ::record-key-cols (s/map-of keyword? ::non-empty-scalar :gen-max 10))
(s/def ::record-val-cols (s/map-of ::col-name any? :gen-max 10))

(s/def ::keyed-rows (s/map-of ::record-key-cols ::record-val-cols :gen-max 10))
(s/def ::unkeyed-rows (s/coll-of ::record-val-cols :gen-max 10))

(s/def ::tables (s/map-of keyword? (s/or ::keyed-rows ::unkeyed-rows) :gen-max 10))

(defn- join-names
  [separator names]
  (->> (map name names)
       (str/join separator)
       (keyword)))

(defn- flatten-paths
  {:derived-from "https://andersmurphy.com/2019/11/30/clojure-flattening-key-paths.html"}
  ([m separator]
   (flatten-paths m separator []))
  ([m separator path]
   (->> (map (fn [[k v]]
               (if (and (map? v) (not-empty v))
                 (flatten-paths v separator (conj path k))
                 [(->> (conj path k)
                       (join-names separator)) v]))
             m)
        (into {}))))

(defn- ->key-cols
  [k]
  (cond
    (map? k) k
    (or (string? k) (keyword? k)) {:name k}
    :else :WTF))

(defn- add-fk
  [rec-m fk-table-name fk-table-key-val]
  (let [col-name (singular fk-table-name)]
    (-> (assoc rec-m col-name fk-table-key-val)
        (with-meta {::columns {col-name {::fk-table-name fk-table-name}}}))))

(def separator "-")

(defn- split-record
  "Accepts a table name and a single record as either a MapEntry or a two-tuple.
  Returns a map of table name to maps representing records."
  [table-name [rec-key rec-m :as _record]]
  (reduce-kv
    (fn [r k v]
      (cond
        (and (coll? v) (map? v))
        (assoc r
              (join-names separator [table-name k])
              (map-keys #(-> (->key-cols %)
                             (add-fk table-name rec-key)) v))
        
        (and (coll? v) (not (map? v)) (map? (first v)))
        (assoc r
              (join-names separator [table-name k])
              (map #(add-fk % table-name rec-key) v))
        
        :else
        (assoc-in r [table-name (->key-cols rec-key) k] v)))
    {}
    rec-m))

(defn recordset->tables
  "Transforms the recordset into one or more tables. The recordset should be either a MapEntry or a
  two-tuple. Returns a map."
  [[rs-name rs-recs :as _recordset]]
  (->> (map-vals #(e/flatten-paths % separator) rs-recs)
       (map #(split-record rs-name %))
       (reduce merge)))

(defn flatten-db
  [db]
  (->> (map recordset->tables db)
       (reduce merge)))

(comment
  ;(require '[dad.db :as db])
  
  (def db-path "/Users/avi.flax/dev/docs/architecture/docs-as-data/db")
  
  (def db (db/read db-path))

  ; (-> db :technologies (select-keys ["Clojure"]))
  
  ; (defn rand-val [m] (-> m seq rand-nth val))
  
  (-> (select-keys db [:technologies])
      (update :technologies #(select-keys % ["Clojure"]))
      (find :technologies)
      recordset->tables)

  (-> (select-keys db [:technologies])
      (update :technologies #(select-keys % ["Clojure"]))
      (find :technologies)
      recordset->tables
      :technologies-recommendations
      first
      meta)
      
  (-> (select-keys db [:technologies])
      (update :technologies #(select-keys % ["Clojure"]))
      (assoc :systems (select-keys (:systems db) ["BILCAS"]))
      (flatten-db)
      (clojure.pprint/pprint))
  
  (s/exercise ::table)
  
  )
