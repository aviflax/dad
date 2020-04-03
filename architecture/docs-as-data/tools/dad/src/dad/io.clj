(ns dad.io
  (:require [clojure.java.io :as io :refer [file]]
            [clojure.string :as str]
            [dad.files :as files :refer [relative-path]]))

(defn- output-path
  [template-path front-path templates-dir out-dir]
  (->> (if front-path
           (file (files/parent-dir template-path) front-path)
           template-path)
       (relative-path templates-dir)
       (str)
       (file out-dir)))

(defn write-docs!
  ;; Accepts a seq of maps containing [:dad/template-path :dad/front-matter :dad/body].
  ;; The value of :dad/front-matter might contain :path which is germaine.
  [rendered-docs out-dir templates-dir repo-root-dir]
  (doseq [[template-path rendered] (group-by :dad/template-path rendered-docs)]
    (print (str "" (relative-path repo-root-dir template-path) "\n└──> "))
    (doseq [{:keys [:dad/body :dad/front-matter :dad/template-path]} rendered
            :let [out-fp (output-path template-path (get front-matter :path) templates-dir out-dir)]]
      (print (str (relative-path repo-root-dir out-fp) "\n     "))
      (io/make-parents out-fp)
      (spit out-fp body))
    (println (str/join (repeat 4 "\b")))))
