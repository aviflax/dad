(ns dad.db.export.sqlite-test
  (:require [clojure.test :refer [deftest is are testing]]
            [dad.db.export.tables :as et]
            [dad.db.export.sqlite :as es]))

(deftest table->ddl
  (are [expected table-name table-rows] (= expected (#'es/table->ddl table-name table-rows))
  
    :technologies
    {"Clojure" {"links-main" "https://clojure.org/"}
     "JRE"     {"summary" "A runtime environment for Java programs."}}
    {:create-table :technologies
     :columns ["name varchar(255) not null primary key"
               "links-main text"
               "summary text"]}

    :technologies-recommendations
    [(with-meta {:technology "Clojure", "type" "assess", "date" "2011-09-15"}
                {::et/columns {:technology {::et/fk-table-name :technologies}}})
     (with-meta {:technology "Clojure", "type" "adopt", "date" "2012-01-12"}
                {::et/columns {:technology {::et/fk-table-name :technologies}}})]
    {:create-table :technologies-recommendations
     :columns ["technology varchar(255) not null references technologies"
               "type text"
               "date text"]}))
