(ns dad.db.export.tables-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]
            [clojure.test :refer [deftest is are testing]]
            [dad.db.export.tables :as et]
            [expound.alpha :as expound]
            [medley.core :as mc :refer [map-entry]]))

; See https://github.com/bhb/expound#printer-options
(set! s/*explain-out* (expound/custom-printer {:print-specs? false}))

(st/instrument `et/add-fk)

(deftest add-fk
  (let [rec-m {"type" "assess"
               "date" "2011-09-15"}
        fk-table-name :technologies
        fk-table-key-val "Clojure"
        expected {:technology "Clojure"
                  "type" "assess"
                  "date" "2011-09-15"}
        expected-meta {::et/columns {:technology {::et/fk-table-name fk-table-name}}}
        res (#'et/add-fk rec-m fk-table-name fk-table-key-val)]
    (is (= expected res))
    (is (= expected-meta (meta res)))))

(deftest split-record
  (are [table-name record expected] (= expected (#'et/split-record table-name record))
    ; table-name
    :technologies
    ; record
    (map-entry "Clojure" {:links-main "https://clojure.org/"
                          "recommendations" [{"type" "assess", "date" "2011-09-15"}
                                             {"type" "adopt", "date" "2012-01-12"}]})
    ; expected
    {:technologies                 {{:name "Clojure"} {:links-main "https://clojure.org/"}}
     :technologies-recommendations [{:technology "Clojure", "type" "assess", "date" "2011-09-15"}
                                    {:technology "Clojure", "type" "adopt", "date" "2012-01-12"}]}

    ; --------------------

    ; table-name
    :systems
    ; record
    (map-entry "Discourse" {:links-main "https://discourse.org/"
                            "containers" {"web"   {"summary" "web server", "technology" "Tomcat"}
                                          "db"    {"summary" "db server", "technology" "Access"}
                                          "cache" {"summary" "hot keys", "technology" "PHP"}}})
    ; expected
    {:systems            {{:name "Discourse"} {:links-main "https://discourse.org/"}}
     :systems-containers {{:system "Discourse" :name "web"}   {"summary" "web server", "technology" "Tomcat"}
                          {:system "Discourse" :name "db"}    {"summary" "db server", "technology" "Access"}
                          {:system "Discourse" :name "cache"} {"summary" "hot keys", "technology" "PHP"}}}

    ; --------------------
    
    ; table-name
    :systems
    ; record
    (map-entry "SACLIB" {"containers" {"API"        {"props" {"marathon-ids" {"kp" "/saclib/api"}}}
                                       "Hutch"      {"props" {"marathon-ids" {"kp" "/saclib/hutch"}
                                                              "technologies" ["RabbitMQ" "Ruby"]}}
                                       "Sidekiq"    {"props" {"marathon-ids" {"kp" "/saclib/sidekiq"}
                                                              "technologies" ["Ruby"]}}
                                       "Web"        {"props" {"marathon-ids" {"kp" "/saclib/web"}}}}
                         "description" "Salad Container Library -- builds libraries of salad containers (duh)"
                         "props"       {"regions"       ["kp"]
                                        "marathon-ids"  {"kp" "/saclib"}
                                        "repos"         ["saclib"]
                                        "related-repos" ["saclib_adapter" "saclib-client"]}})
    ; expected
    {:systems            {{:name "SACLIB"} {"description"   "Salad Container Library -- builds libraries of salad containers (duh)"
                                            "regions"       ["kp"]
                                            "marathon-ids"  {"kp" "/saclib"}
                                            "repos"         ["saclib"]
                                            "related-repos" ["saclib_adapter" "saclib-client"]}}
     :systems-containers {{:system "SACLIB" :name "API"}     {"marathon-ids-kp" "/saclib/api"}
                          {:system "SACLIB" :name "Hutch"}   {"marathon-ids-kp" "/saclib/hutch"
                                                              "technologies"    ["RabbitMQ" "Ruby"]}
                          {:system "SACLIB" :name "Sidekiq"} {"marathon-ids-kp" "/saclib/sidekiq"
                                                              "technologies"    ["Ruby"]}
                          {:system "SACLIB" :name "Web"}     {"marathon-ids-kp" "/saclib/web"}}})
                          
  (let [table-name :technologies
        record (map-entry "Clojure" {:links-main "https://clojure.org/"
                                     "recommendations" [{"type" "assess", "date" "2011-09-15"}
                                                        {"type" "adopt", "date" "2012-01-12"}]})
        res (#'et/split-record table-name record)]
    (is (s/valid? ::et/tables res) (s/explain-str ::et/tables res))))

(deftest recordset->tables
  (let [recordset (map-entry :technologies {"Clojure" {"links" {"main" "https://clojure.org/"}
                                                       "recommendations" [{"type" "assess", "date" "2011-09-15"}
                                                                          {"type" "adopt", "date" "2012-01-12"}]}})
        expected {:technologies {{:name "Clojure"} {:links-main "https://clojure.org/"}}
                  :technologies-recommendations [{:technology "Clojure", "type" "assess", "date" "2011-09-15"}
                                                 {:technology "Clojure", "type" "adopt", "date" "2012-01-12"}]}
        res (#'et/recordset->tables recordset)]
  (is (= expected res))
  (is (s/valid? ::et/tables res) (s/explain-str ::et/tables res))
  (is (= {::et/columns {:technology {::et/fk-table-name :technologies}}}
         (meta (first (:technologies-recommendations res)))))))
