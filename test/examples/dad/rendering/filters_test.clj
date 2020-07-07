(ns dad.rendering.filters-test
  (:require [clojure.test :refer [are deftest is]]
            [dad.rendering.filters :as filters :refer [filters]])
  (:import [java.time.format DateTimeParseException]))

(deftest latest-by
  (let [f (:latest-by filters)]
    ;; happy paths
    (are [maps key-path expected] (is (= expected (f maps key-path)))
    
      ; shallow maps, single-segment key-path
      [{"mission" "Dragon qualification"
        "date"    "2010-06-04"}
       {"mission" "Dragon demo C102"
        "date"    "2012-05-22"}
       {"mission" "Dragon demo C101"
        "date"    "2010-12-08"}]
      "date"
      {"mission" "Dragon demo C102"
       "date"    "2012-05-22"}
      
      ; nested maps, multi-segment key-path
      [{"mission" "Dragon qualification"
        "launch"  {"date" "2010-06-04"}}
       {"mission" "Dragon demo C102"
        "launch"  {"date" "2012-05-22"}}
       {"mission" "Dragon demo C101"
        "launch"  {"date" "2010-12-08"}}]
      "launch.date"
      {"mission" "Dragon demo C102"
       "launch"  {"date" "2012-05-22"}}

      ; coll of 1
      [{"mission" "Dragon qualification"
        "date"    "2010-06-04"}]
      "launch-date-foo-does-not-matter"
      [{"mission" "Dragon qualification"
        "date"    "2010-06-04"}]

      ; empty coll
      []
      "foo"
      []
      
      ; nil coll
      nil
      "foo"
      nil)

    ;; sad paths resulting in AssertionError
    (are [maps key-path] (is (thrown? AssertionError (f maps key-path)))
      ; first arg not a coll
      "foo"
      "launch.date"
      
      ; second arg not a string
      [{"foo" "bar"}]
      [:foo :bar]
      
      ; second arg blank
      [{"foo" "bar"}]
      "")

    ;; sad paths resulting in NPE
    (are [maps key-path] (is (thrown? NullPointerException (f maps key-path)))
      ; no match on key-path
      [{"mission" "Dragon qualification"
        "date"    "2010-06-04"}
       {"mission" "Dragon demo C102"
        "date"    "2012-05-22"}
       {"mission" "Dragon demo C101"
        "date"    "2010-12-08"}]
      "launch.date"
      
      [1 2 3 4]
      "launch.date")
    
    ;; sad paths resulting in DateTimeParseException
    (are [maps key-path] (is (thrown? DateTimeParseException (f maps key-path)))
      [{"mission" "Dragon qualification"
        "date"    "2010-06-04"}
       {"mission" "Dragon demo C102"
        "date"    "2012454325ABC!-05-22"}
       {"mission" "Dragon demo C101"
        "date"    "2010-12-08"}]
      "date")))

(deftest with
  (let [f (:with filters)]
    ;; happy paths
    (are [coll k expected] (is (= expected (f coll k)))
      ;; map of maps
      {"Clojure" {"recommended" {"by" "Avi", "on" "2010-12-08"}}
       "PHP"     {"deprecated"  {"by" "Avi", "on" "2010-12-08"}}}
      "recommended"
      {"Clojure" {"recommended" {"by" "Avi", "on" "2010-12-08"}}}
      
      ;; non-associative coll of maps
      [{"name" "Clojure", "recommended" {"by" "Avi", "on" "2010-12-08"}}
       {"name" "PHP",     "deprecated"  {"by" "Avi", "on" "2010-12-08"}}]
      "recommended"
      [{"name" "Clojure", "recommended" {"by" "Avi", "on" "2010-12-08"}}]
      
      ;; empty coll
      []
      "foo"
      []
      
      ;; nil
      nil
      "foo"
      nil)
    
    ;; sad paths
    (are [coll k] (is (thrown? AssertionError (f coll k)))
      ;; vals are not maps
      [1 2 3 4]
      "recommended"
      
      ;; vals are not maps
      {"Clojure" true
       "PHP"     false}
      "recommended"

      ;; nil key
      {"Clojure" {"recommended" {"by" "Avi", "on" "2010-12-08"}}
       "PHP"     {"deprecated"  {"by" "Avi", "on" "2010-12-08"}}}
      nil
      
      ;; blank key
      {"Clojure" {"recommended" {"by" "Avi", "on" "2010-12-08"}}
       "PHP"     {"deprecated"  {"by" "Avi", "on" "2010-12-08"}}}
      "")))
