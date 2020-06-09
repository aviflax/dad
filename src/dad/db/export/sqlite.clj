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

(defn- recordset->table
  "On success, returns nil. On failure, returns "
  [[rs-name rs-recs :as _recordset] ds]
  (let [rows (e/flatten-paths rs-recs map-path-separator)]
    (create-table rs-name rows ds)
    (insert-rows rs-name rows ds)
    ))

(defn db->sqlite
  "On success, returns true."
  [dad-db db-out-path]
  (let [db-spec {:dbtype "sqlite" :dbname db-out-path}
        ds (j/get-datasource db-spec)]
    (doseq [recordset dad-db]
      (recordset->table recordset ds))
    nil))

(comment
  
  
  )
