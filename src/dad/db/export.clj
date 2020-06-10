(ns dad.db.export
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [dad.db.export :as e]
            [medley.core :as mc :refer [map-vals]]))

(defn- flatten-paths
  {:source "https://andersmurphy.com/2019/11/30/clojure-flattening-key-paths.html"}
  ([m separator]
   (flatten-paths m separator []))
  ([m separator path]
   (->> (map (fn [[k v]]
               (if (and (map? v) (not-empty v))
                 (flatten-paths v separator (conj path k))
                 [(->> (conj path k)
                       (map name)
                       (str/join separator)
                       keyword) v]))
             m)
        (into {}))))

(defn- split-record
  "Accepts a table name and a map representing a single record, as a MapEntry.
   Returns a map of table name to maps representing records."
  [table-name [rec-key rec-m :as _record]]
  (reduce-kv
    (fn [r k v]
      (if (and (coll? v)
               (map? (first v)))
        (assoc r k (zipmap (drop 1 (range)) v))
        (assoc-in r [table-name rec-key k] v)))
    {}
    rec-m))

(def map-path-separator "-")

(defn recordset->tables
  "Transforms the recordset into one or more tables. The recordset should either a MapEntry or a
  two-tuple. Returns a map."
  [[rs-name rs-recs :as _recordset]]
  (let [rows (map-vals #(e/flatten-paths % map-path-separator) rs-recs)
        rows2 (map #(split-record rs-name %) rows)]
    rows2))

(comment
  ;(require '[dad.db :as db])
  
  (def db-path "/Users/avi.flax/dev/docs/architecture/docs-as-data/db")
  
  (def db (db/read db-path))

  ; (-> db :technologies (select-keys ["Clojure"]))
  
  ; (defn rand-val [m] (-> m seq rand-nth val))
  
  (-> (select-keys db [:technologies])
      (update :technologies #(select-keys % ["Clojure"]))
      (find :technologies)
      (recordset->tables))
    
  )
