(ns dad.cli
  (:require [clojure.string :as str]
            [dad.db :as db]
            [dad.io :as io]
            [dad.rendering :as r]))

(defn -main
  [& [data-dir templates-dir out-dir repo-root-dir :as _args]]
  ;; TODO: this is primitive; switch to clojure.tools.cli
  (when (some str/blank? [data-dir templates-dir out-dir])
    (println (str "ERROR one or more required arguments are missing or blank.\n"
                  "USAGE: <program> DATA-PATH TEMPLATES-PATH OUT-PATH"))
    (System/exit 1))
  ; (println "args:" args) ; TODO: add a debug flag
  (-> (db/read data-dir)
      (r/build-docs templates-dir repo-root-dir)
      (io/write-docs! out-dir templates-dir repo-root-dir)))
