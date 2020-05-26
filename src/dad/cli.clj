(ns dad.cli
  (:require [cli-matic.core :refer [run-cmd]]
            [clojure.java.io :refer [file]]
            [dad.db :as db]
            [dad.io :as io]
            [dad.rendering :as r]))

(defn- exists?!
  [opt-name s]
  (let [f (file s)]
    (when-not (.isDirectory f)
      (throw (RuntimeException. (format "The path supplied for %s, %s, must be a directory!"
                                        opt-name
                                        s))))))

(defn- on-change
  [paths f]
  :TODO)

(defn- render
  [{:keys [db templates project-root out] :as _opts}]
  (-> (db/read db)
      (r/build-docs templates project-root)
      (io/write-docs! out templates project-root)))

(defn- render-command
  [{:keys [db templates watch] :as opts}]
  (run! (fn [[opt-name s]] (exists?! opt-name s))
        (select-keys opts [:db :templates :project-root :out]))
  (if watch
    (on-change [db templates] #(render opts))
    (render opts)))

(defn- rebuild
  [{db :db}]
  (exists?! :db db)
  (-> (db/read db)
      (db/overwrite)))

(def db-opt
  {:option  "db"
   :as      "The path to the database directory"
   :type    :string
   :default :present})

(def config
  ;; The spec for this is here: https://github.com/l3nz/cli-matic/blob/master/README.md
  ;; :default :present means required ¯\_(ツ)_/¯
  {:app         {:command     "dad"
                 :description "Toolkit for “Docs as Data”"
                 :version     "TBD"}
   :commands    [{:command     "render"
                  :description (str "Generates document files as per the document templates and the"
                                    " data in the database.")
                  :opts        [db-opt
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
                                 :default :present}
                                {:option  "watch"
                                 :as      "Stay resident and re-render templates when they change"
                                 :type    :with-flag
                                 :default false}]
                   :runs        render-command}
                  {:command     "rebuild" ; Maybe rename to `reformat?`
                   :description (str "Reads the database and writes it right back out, normalizing"
                                     " the formatting.")
                   :opts        [db-opt
                                 {:option  "watch"
                                  :as      "Stay resident and rebuild the db when any of its files change"
                                  :type    :with-flag
                                  :default false}]
                   :runs        rebuild}]})

(defn -main
  [& args]
  (run-cmd args config))
