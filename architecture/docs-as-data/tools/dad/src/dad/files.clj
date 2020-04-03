(ns dad.files
  (:require [clojure.java.io :refer [file]])
  (:import [java.nio.file Path]))

(defn absolute-path
  "Accepts a path as a Path, File, or String; returns an absolute Path."
  [fp]
  (-> (if (instance? Path fp)
        fp
        (.toPath (file fp)))
      (.toAbsolutePath)))

(defn parent-dir
  [fp]
  (.getParent (.getAbsoluteFile (file fp))))

(defn relative-path
  "Accepts paths as Path, File, or String objects; returns a relative Path."
  [from to]
  (.relativize (absolute-path from) (absolute-path to)))
