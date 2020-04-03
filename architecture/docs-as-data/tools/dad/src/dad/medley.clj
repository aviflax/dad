(ns dad.medley
  (:require [clojure.string :refer [lower-case]]))

(def case-insensitive-sorted-map
  "Be careful when using this! If any keys are the same except for casing, one will be lost."
  (sorted-map-by (fn [a b] (compare (lower-case a) (lower-case b)))))
