(ns dad.db.export-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]
            [clojure.test :refer [deftest is are testing]]
            [dad.db.export :as e]
            [expound.alpha :as expound]
            [medley.core :as mc :refer [map-entry]]))

; See https://github.com/bhb/expound#printer-options
(set! s/*explain-out* (expound/custom-printer {:print-specs? false}))

(st/instrument `e/add-fk)

(deftest add-fk
  (let [rec-m {"type" "assess"
               "date" "2011-09-15"}
        fk-table-name :technologies
        fk-table-key-val "Clojure"
        expected {:technology "Clojure"
                  "type" "assess"
                  "date" "2011-09-15"}
        expected-meta {::e/columns {:technology {::e/fk-table-name fk-table-name}}}
        res (#'e/add-fk rec-m fk-table-name fk-table-key-val)]
    (is (= expected res))
    (is (= expected-meta (meta res)))))

(deftest split-record
  (are [table-name record expected] (= expected (#'e/split-record table-name record))
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
     :systems-containers {{:name "web", :system "Discourse"}   {"summary" "web server", "technology" "Tomcat"}
                          {:name "db", :system "Discourse"}    {"summary" "db server", "technology" "Access"}
                          {:name "cache", :system "Discourse"} {"summary" "hot keys", "technology" "PHP"}}})
                          
  (let [table-name :technologies
        record (map-entry "Clojure" {:links-main "https://clojure.org/"
                                     "recommendations" [{"type" "assess", "date" "2011-09-15"}
                                                        {"type" "adopt", "date" "2012-01-12"}]})
        res (#'e/split-record table-name record)]
    (is (s/valid? ::e/tables res) (s/explain-str ::e/tables res))))

(deftest recordset->tables
  (let [recordset (map-entry :technologies {"Clojure" {"links" {"main" "https://clojure.org/"}
                                                       "recommendations" [{"type" "assess", "date" "2011-09-15"}
                                                                          {"type" "adopt", "date" "2012-01-12"}]}})
        expected {:technologies {{:name "Clojure"} {:links-main "https://clojure.org/"}}
                  :technologies-recommendations [{:technology "Clojure", "type" "assess", "date" "2011-09-15"}
                                                 {:technology "Clojure", "type" "adopt", "date" "2012-01-12"}]}
        res (#'e/recordset->tables recordset)]
  (is (= expected res))
  (is (s/valid? ::e/tables res) (s/explain-str ::e/tables res))
  (is (= {::e/columns {:technology {::e/fk-table-name :technologies}}}
         (meta (first (:technologies-recommendations res)))))))
