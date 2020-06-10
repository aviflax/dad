(ns dad.db.export.sqlite
  (:require [clojure.spec.alpha :as s]
            [cognitect.anomalies :as anom]
            [dad.db.export :as e]
            [medley.core :as mc :refer [map-vals]]
            [next.jdbc :as j]
            [next.jdbc.sql :as sql]))

(defn- create-table
  [name rows ds]
  nil)

(def map-path-separator "-")

(defn- insert-rows
  [rs-name rows ds]
  nil)

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

(defn- recordset->tables
  "Transforms the recordset into one or more tables. The recordset should either a MapEntry or a
  two-tuple. Returns a map."
  [[rs-name rs-recs :as _recordset]]
  (let [rows (map-vals #(e/flatten-paths % map-path-separator) rs-recs)
        rows2 (map #(split-record rs-name %) rows)]
    rows2))

(defn db->sqlite
  "On success, returns true."
  [dad-db db-out-path]
  (let [db-spec {:dbtype "sqlite" :dbname db-out-path}
        ds (j/get-datasource db-spec)]
    (doseq [recordset dad-db]
      (recordset->tables recordset))
    nil))

(comment
  ;(require '[dad.db :as db])
  
  (def db-path "/Users/avi.flax/dev/docs/architecture/docs-as-data/db")
  
  (def db (db/read db-path))
  (keys db)
  (-> db :technologies (select-keys ["Clojure"]))
  
  (defn rand-val [m] (-> m seq rand-nth val))
  
  (-> (select-keys db [:technologies])
      (update :technologies #(select-keys % ["Clojure"]))
      (find :technologies)
      (recordset->tables))
    
  )
