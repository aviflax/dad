(ns dad.db.export
  (:require [clojure.string :as str]))

(defn flatten-paths
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
