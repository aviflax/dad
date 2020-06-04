(ns dad.rendering.tags-test
  (:require [clojure.test :refer [are deftest is]]
            [dad.rendering.tags :as tags]
            [selmer.parser :as sp]))

(deftest test-exec
  (tags/register!)

  (are [expected template] (= expected (sp/render template {}))
    ; Happy paths
    "foo bar"         "{% exec echo -n foo %} bar"
    "LW4gZm9vCg==\n"  "{% exec /bin/sh -c \"echo -n foo | base64\" %}")

  (is (thrown? Exception (sp/render "{% exec foo echo %}" {})))
  (is (thrown? Exception (sp/render "{% exec /bin/sh -c 'exit 1' %}" {}))))

(deftest test-removeblanklines
  (tags/register!)
  (are [expected template] (= expected (sp/render template {}))
    "Foo\nbar"
    "{% removeblanklines %}Foo\n\nbar{% endremoveblanklines %}"
    
    "Foo\nbar\n"
    "{% removeblanklines %}Foo\n\nbar\n\n\n\n{% endremoveblanklines %}"
    
    "\nFoo\nbar\n"
    "{% removeblanklines %}\n\n\n\nFoo\n\n\n\nbar\n\n\n\n{% endremoveblanklines %}"))
