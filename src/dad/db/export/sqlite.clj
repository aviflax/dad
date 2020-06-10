(ns dad.db.export.sqlite
  (:require ;[clojure.spec.alpha :as s]
            ;[cognitect.anomalies :as anom]
            [dad.db.export :as e]
            ;[medley.core :as mc :refer [map-vals]]
            [next.jdbc :as j]
            [next.jdbc.sql :as sql]))

(defn- create-table
  [name rows ds]
  nil)

(defn- insert-rows
  [rs-name rows ds]
  nil)

(defn table->ddl
  "Accepts a ::dad.db.export/table, returns a ::dad.db.export/ddl-statement"
  [table-name rows]
  nil)

(defn db->sqlite
  "On success, returns true."
  [flattened-dad-db db-out-path]
  (let [out-db-spec {:dbtype "sqlite" :dbname db-out-path}
        out-ds (j/get-datasource out-db-spec)]
    :TODO))
