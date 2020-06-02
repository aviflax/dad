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

(deftest test-collapseblanklines
  (tags/register!)
  (are [expected template] (= expected (sp/render template {}))
    "Foo\nbar"
    "{% collapseblanklines %}Foo\n\nbar{% endcollapseblanklines %}"
    
    "Foo\nbar\n"
    "{% collapseblanklines %}Foo\n\nbar\n\n\n\n{% endcollapseblanklines %}"))

(deftest test-replace
  (tags/register!)
  (are [expected template] (= expected (sp/render template {}))
    ;;;; Happy paths

    ; Simple
    "Foo\nbar"
    "{% replace \"\n\n\" \"\n\" %}Foo\n\nbar{% endreplace %}"
    
    ;; NOT WORKING
    ; "Qoo\nbar"
    ; "{% replace \"\\n\\n\" \"\\n\" %}Qoo\n\nbar{% endreplace %}"

    ; No quotes — NOT WORKING
    ; "Goo\nbar"
    ; "{% replace \n\n \n %}Goo\n\nbar{% endreplace %}"
    
    ; Using a regex feature
    "Foo\nbar"
    "{% replace \"\n{2,}\" \"\n\" %}Foo\n\n\n\n\n\n\n\n\n\n\n\n\n\n\nbar{% endreplace %}"
    
    ;;;; Sad paths

    ; No match found
    "Foo\n\nbar"
    "{% replace foo bar %}Foo\n\nbar{% endreplace %}"
    
    ; Missing arg
    "ERROR: The DaD tag `replace` requires two args: `this` and `that` as in “replace this with that”."
    "{% replace foo %}Foo\n\nbar{% endreplace %}"))
