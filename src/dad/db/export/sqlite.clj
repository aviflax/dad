(ns dad.db.export.sqlite
  (:require [clojure.spec.alpha :as s]
            [cognitect.anomalies :as anom]
            [dad.db.export :as e]
            [next.jdbc :as j]
            [next.jdbc.sql :as sql]))

(defn- create-table
  [name rows ds]
  nil)

(def map-path-separator "-")

(defn- insert-rows
  [rs-name rows ds]
  nil)

(defn- recordset->tables
  "Transforms the recordset into one or more tables. The recordset should either a MapEntry or a
  two-tuple. Returns a map."
  [[rs-name rs-recs :as _recordset]]
  (let [rows (e/flatten-paths rs-recs map-path-separator)]
    nil))

(defn db->sqlite
  "On success, returns true."
  [dad-db db-out-path]
  (let [db-spec {:dbtype "sqlite" :dbname db-out-path}
        ds (j/get-datasource db-spec)]
    (doseq [recordset dad-db]
      (recordset->tables recordset ds))
    nil))

(comment
  ;(require '[dad.db :as d])
  
  (def db-path "/Users/avi.flax/dev/docs/architecture/docs-as-data/db")
  
  (def db (d/read db-path))
  (keys db)
  (-> db :technologies (select-keys ["Clojure"]))
  
  (defn rand-val [m] (-> m seq rand-nth val))
  
  (e/flatten-paths (-> db rand-val rand-val) map-path-separator)
  )
