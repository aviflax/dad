(ns dad.files
  (:require [clojure.java.io :refer [file]])
  (:import [java.nio.file Path]))

(set! *warn-on-reflection* true)

(defn absolute-path
  "Accepts a path as a Path, File, or String; returns an absolute Path."
  ^Path
  [fp]
  (let [^Path p (if (instance? Path fp)
                  fp
                  (.toPath (file fp)))]
    (.toAbsolutePath p)))

(defn parent-dir
  [fp]
  (.getParent (.getAbsoluteFile (file fp))))

(defn relative-path
  "Accepts paths as Path, File, or String objects; returns a relative Path."
  [from to]
  (.relativize (absolute-path from) (absolute-path to)))
