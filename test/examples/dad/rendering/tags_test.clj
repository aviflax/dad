(ns dad.rendering.tags-test
  (:require [clojure.test :refer [are deftest]]
            [dad.rendering.tags :as tags]
            [selmer.parser :as sp]))

(deftest test-shell
  (tags/register!)
  (are [expected template] (= expected (sp/render template {}))
    "foo bar"         "{% sh echo -n foo %} bar"                       ; shell builtin
    "LW4gZm9vCg==\n"  "{% sh /bin/sh -c \"echo -n foo | base64\" %}"   ; not a shell builtin
    
    ; Sad paths
    (str "Command » foo echo « failed: java.io.IOException:"
         " Cannot run program \"foo\": error=2, No such file or directory")  "{% sh foo echo %}"))
