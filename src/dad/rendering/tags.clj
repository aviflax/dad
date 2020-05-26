(ns dad.rendering.tags
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [selmer.parser :as sp])
  (:import [java.io IOException]))

(defn- unwrap
  "When Selmer parses tag args, it respects strings that are bounded by double-quotes — such strings
  are preserved as scalar values. But for some reason it includes those double-quotes as part of the
  values. We don’t want them, so this removes them."
  [s]
  (if (and (str/starts-with? s "\"")
           (str/ends-with? s "\""))
    (subs s 1 (dec (count s)))
    s))

(defn- sh
  [args _context]
  (try (let [{:keys [exit out err] :as _res} (apply shell/sh (map unwrap args))
             err-msg "Command » %s « failed:\nexit code: %s\nstdout: %s\nstderr: %s\n\n"]
         (if (zero? exit)
           out
           (format err-msg (str/join " " args) exit out err)))
       (catch IOException e
         (format "Command » %s « failed: %s" (str/join " " args) e))))

(def tags
  {:sh sh})

(defn register!
  []
  (doseq [[name f] tags]
    (sp/add-tag! name f)))
