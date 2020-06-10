(ns dad.db.export-test
  (:require [clojure.test :refer [deftest is are testing]]
            [dad.db.export :as e]))

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

(deftest recordset->tables
  (let [recordset {:technologies {"Clojure" {"links" {"main" "https://clojure.org/"}
                                             "recommendations" [{"type" "assess"
                                                                 "date" "2011-09-15"}
                                                                {"type" "adopt"
                                                                 "date" "2012-01-12"}]}}}
        expected {:technologies {"Clojure" {"links-main" "https://clojure.org/"}}
                  :technologies-recommendations [{:technology "Clojure"
                                                  "type" "assess"
                                                  "date" "2011-09-15"}
                                                 {:technology "Clojure"
                                                  "type" "adopt"
                                                  "date" "2012-01-12"}]}
        res (#'e/recordset->tables recordset)]
  (is (= expected res))))
