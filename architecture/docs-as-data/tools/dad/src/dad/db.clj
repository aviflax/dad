(ns dad.db
  "A DaD database is, logically, a map:

  database: dataset-name->dataset
  dataset-name: keyword
  dataset: record-id->record-properties
  record-properties: prop-key->prop-val
  prop-key: any valid simple YAML value
  prop-val: any valid simple YAML value"
  (:refer-clojure :exclude [read])
  (:require [dad.medley :as m]
            [dad.yaml :as yaml]
            [clojure.java.io :as io]
            [medley.core :as mc :refer [deep-merge map-vals]]))

(defn read-physical
  "Returns a map of filepath to the parsed YAML contents of the file. NB: a single dataset may be
  spread across multiple files! Be careful not to assume that a file and a dataset have a 1:1
  relationship because some do not.

  When parsing the YAML files, we do *not* keywordize all keys because record keys should be left
  alone, as-is. It just causes too many problems when they’re changed in any way."
  [data-dir]
  (let [files (yaml/file-seq data-dir)]
    (zipmap files
            (map #(yaml/parse-string (slurp %) :keywords false) files))))

(def warning-header
  (str "# PLEASE NOTE: you are welcome to edit this data, but please be aware\n"
       "#              that this file is regularly reprocessed programmatically,\n"
       "#              so comments and manual formatting changes will be lost.\n\n"))

(defn overwrite-physical
  "Accepts a map of filepath to the datasets (or partial datasets) that should be written to that
  file. Will overwrite all files. Does not delete any files."
  [db]
  (->> db
       (map-vals yaml/stringify)
       (map-vals #(str warning-header %))
       (run! (fn [[path str]]
               (io/make-parents path)
               (spit path str))))
  nil)

(defn- sort-rs-value-maps
  "Recordsets are maps of keys to values. The values themselves are maps, of, essentially, property
  name to property value. We want those value maps to be sorted, case-insensitively. Ideally those
  property names should be all lower-case, but just in case someone *does* use an upper-case char in
  a name, we want the sort to be case-insensitive."
  [rs-map]
  (mc/map-vals #(into m/case-insensitive-sorted-map %) rs-map))

(defn- add-meta
  [fp file-root-val]
  {:pre [(map? file-root-val)]}
  (map-vals
    (fn [recordset]
      (map-vals
        (fn [v] (if (instance? clojure.lang.IObj v)
                    (with-meta v {::file fp})
                    v))
        recordset))
    file-root-val))

(defn read
  "Returns the db as a single deep-merged map. In other words, datasets that are spread across
  multiple YAML files are merged into a unified dataset. Each record in the dataset will have a
  metadata map containing the key ::file, the value of which is a File object wrapping the
  absolute path to the file that does, should, or will contain that record."
  [data-dir]
  (->> (read-physical data-dir)
       (map (fn [[fp parsed-file-content]]
              (->> parsed-file-content
                   (mc/map-keys keyword) ; recordset names are the only keys that are keywordized
                   (mc/map-vals sort-rs-value-maps)
                   (add-meta fp))))
       (reduce deep-merge)))

(defn- get-file
  [record]
  ;; TODO: remove the `or` and the `throw` they’re just shortcutty hacks
  (or (io/file (::file (meta record)))
      (throw (ex-info "Key :dad.db/file not found in record metadata." {:record record :record-meta (meta record)}))))

(def default-sort "case-insensitive-alphabetical")

(defn- logical->physical
  [db]
  (reduce
    (fn [file-maps [ds-name ds-recs]]
      (->> ds-recs

           ; result file to records (as seq of kv tuples)
           (group-by (fn [[_rec-key rec-props]] (get-file rec-props)))

           ; convert those record seqs from seqs of kv tuples back into maps
           (map-vals #(into (if (= (get-in db [:schema (name ds-name) "sort"] default-sort) "manual")
                                {}
                                m/case-insensitive-sorted-map)
                            %))

           ; wrap the record maps in a map, making each value a well-formed dataset
           (map-vals (fn [ds-recs] {ds-name ds-recs}))

           ;; now merge this set of files and datasets into our accumulating physical-db result
           (deep-merge file-maps)))
    {}
    db))

(defn overwrite
  "Accepts the entire DB and overwrites the entire DB. EXCEPT: if a dataset that exists in the files
  on disk is NOT present in the input db, it will NOT be deleted from the disk; that file won’t be
  modified at all. TODO, I guess.

  If any record does not contain a metadata map with the key ::file then nothing will be written
  and the fn will return an anomaly map. (TODO)

  If all goes well, returns {:success true}."
  [db]
  (->> (logical->physical db)
       (overwrite-physical))
  {:success true})

(defn upsert
  "Accepts a full DB and a partial db — i.e. a map of recordsets — merges the partial db into the
  full db, and writes the result to disk.

  If any record does not contain a metadata map with the key :file then nothing will be written
  and the fn will return an anomaly map. (TODO)

  If all goes well, returns {:success true}."
  [db partial-db]
  (-> (mc/deep-merge db partial-db)
      (overwrite))
  {:success true})
