(ns dad.db.export.sqlite-test
  (:require [clojure.test :refer [deftest is are testing]]
            [dad.db.export.sqlite :as es]))

(deftest recordset->tables
  (let [recordset {:technologies {"Clojure" {"links" {"main" "https://clojure.org/"}
                                             "recommendations" [{"type" "assess"
                                                                 "date" "2011-09-15"}
                                                                {"type" "adopt"
                                                                 "date" "2012-01-12"}]}}}
        expected {:technologies {"Clojure" {"links-main" "https://clojure.org/"}}
                  :technologies-recommendations [{"technology" "Clojure"
                                                  "type" "assess"
                                                  "date" "2011-09-15"}
                                                 {"technology" "Clojure"
                                                  "type" "adopt"
                                                  "date" "2012-01-12"}]}
        expected-meta {:ddl {:technologies
                             ["name varchar(255) not null primary key"
                              "links-main text"]

                             :technologies-recommendations
                             ["id int auto_increment primary key"
                              "technology varchar(255) not null references technologies"
                              "type text"
                              "date text"]}}
        res (es/recordset->tables recordset)]
  (is (= expected res))
  (is (= expected-meta (select-keys (meta res) [:ddl])))))
