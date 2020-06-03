(ns dad.rendering.tags-test
  (:require [clojure.test :refer [are deftest]]
            [dad.rendering.tags :as tags]
            [selmer.parser :as sp]))

(deftest test-exec
  (tags/register!)
  (are [expected template] (= expected (sp/render template {}))
    ; Happy paths
    "foo bar"         "{% exec echo -n foo %} bar"
    "LW4gZm9vCg==\n"  "{% exec /bin/sh -c \"echo -n foo | base64\" %}"
    
    ; Sad paths
    (str "Command » foo echo « failed: java.io.IOException:"
         " Cannot run program \"foo\": error=2, No such file or directory")  "{% exec foo echo %}"))

(deftest test-removeblanklines
  (tags/register!)
  (are [expected template] (= expected (sp/render template {}))
    "Foo\nbar"
    "{% removeblanklines %}Foo\n\nbar{% endremoveblanklines %}"
    
    "Foo\nbar\n"
    "{% removeblanklines %}Foo\n\nbar\n\n\n\n{% endremoveblanklines %}"
    
    "\nFoo\nbar\n"
    "{% removeblanklines %}\n\n\n\nFoo\n\n\n\nbar\n\n\n\n{% endremoveblanklines %}"))
