(ns dad.cli
  (:require [cli-matic.core :refer [run-cmd]]
            [clojure.java.io :refer [file]]
            [dad.db :as db]
            [dad.io :as io]
            [dad.rendering :as r]))

(defn- exists?!
  [[opt-name s]]
  (let [f (file s)]
    (when-not (.isDirectory f)
      (throw (RuntimeException. (format "The path supplied for %s, %s, must be a directory!"
                                        opt-name
                                        s))))))

(defn- build
  [{:keys [db templates project-root out] :as opts}]
  (run! exists?! (select-keys opts [:db :templates :project-root :out]))
  (-> (db/read db)
      (r/build-docs templates project-root)
      (io/write-docs! out templates project-root)))

(def config
  ;; The spec for this is here: https://github.com/l3nz/cli-matic/blob/master/README.md
  ;; :default :present means required ¯\_(ツ)_/¯
  {:app         {:command     "dad"
                 :description "Toolkit for “Docs as Data”"
                 :version     "TBD"}
   :commands    [{:command     "build"
                  :description (str "Generates document files as per the document templates and the"
                                    " data in the database.")
                  :opts        [{:option  "db"
                                 :as      "The path to the database directory"
                                 :type    :string
                                 :default :present}
                                {:option  "templates"
                                 :as      "The path to the templates directory"
                                 :type    :string
                                 :default :present}
                                {:option  "project-root"
                                 :as      "The path to directory that is the root of the project"
                                 :type    :string
                                 :default :present}
                                {:option  "out"
                                 :as      (str "The path to directory to which the generated"
                                               " documents should be written")
                                 :type    :string
                                 :default :present}]
                   :runs build}]})

(defn -main
  [& args]
  (run-cmd args config))
