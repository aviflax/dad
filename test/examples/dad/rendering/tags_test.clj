(ns dad.rendering.tags-test
  (:require [clojure.test :refer [are deftest is]]
            [dad.rendering.tags :as tags]
            [selmer.parser :as sp])
  (:import [clojure.lang ExceptionInfo]
           [java.io IOException]))

(deftest test-exec
  (tags/register!)

  (are [expected template] (= expected (sp/render template {}))
    ; Happy paths
    "foo bar"         "{% exec echo -n foo %} bar"
    "Zm9vCg==\n"  "{% exec /bin/sh -c \"echo foo | base64\" %}")

  (is (thrown-with-msg? IOException
                        #"^Cannot run program.+foo.+No such file"
                        (sp/render "{% exec foo echo %}" {})))

  (is (thrown-with-msg? ExceptionInfo
                        #"^Command.+sh.+failed"
                        (sp/render "{% exec /bin/sh -c 'exit 1' %}" {}))))

(deftest test-removeblanklines
  (tags/register!)
  (are [expected template] (= expected (sp/render template {}))
    "Foo\nbar"
    "{% removeblanklines %}Foo\n\nbar{% endremoveblanklines %}"
    
    "Foo\nbar\n"
    "{% removeblanklines %}Foo\n\nbar\n\n\n\n{% endremoveblanklines %}"
    
    "\nFoo\nbar\n"
    "{% removeblanklines %}\n\n\n\nFoo\n\n\n\nbar\n\n\n\n{% endremoveblanklines %}"))
