(ns dad.validation
  (:require [scjsv.core :as v]))


(defn- f
  [[rs-name rs-val] db]
  (let [schema (get rs-val "json-schema")
        validator (v/validator schema)
        rs (get db rs-name)]
    (validator rs)))

(defn validate
  "Returns a sequence of errors, with some TBD shape, or nil, or maybe an empty sequence?"
  [db]
  (->> (:schemata db)
       (filter #(and (map? %)
                     (contains? % "json-schema")))
       (map #(f db %))))
