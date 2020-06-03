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
  (if (and (string? s)
           (str/starts-with? s "\"")
           (str/ends-with? s "\""))
    (subs s 1 (dec (count s)))
    s))

(def ^:private remove-blank-lines
  "A full custom tag “spec” as required by selmer.parser/add-tag! — a sequence of either
  2 or 3 values; in this case, 3 values: the opening tag, the fn, and the closing tag. This is all
  packaged together because the function needs to “know” the tag name that’s used for it, so that
  it can retrieve the content it needs. (This is how Selmer works; I don’t know why). I wanted to
  keep that all “local” so it wouldn’t be spread around in a few places. This way, if we change the
  name of the tag, all the changes will be localized in this one place."
  (let [opening-tag :removeblanklines
        closing-tag :endremoveblanklines]
    (vector opening-tag
            (fn removeblanklines [_args _context content]
              (str/replace (get-in content [opening-tag :content]) #"\n{2,}" "\n"))
            closing-tag)))

(defn- exec
  [args _context]
  (try (let [{:keys [exit out err] :as _res} (apply shell/sh (map unwrap args))
             err-msg "Command » %s « failed:\nexit code: %s\nstdout: %s\nstderr: %s\n\n"]
         (if (zero? exit)
           out
           (format err-msg (str/join " " args) exit out err)))
       (catch IOException e
         (format "Command » %s « failed: %s" (str/join " " args) e))))

(def tags
  [remove-blank-lines
   [:exec exec]])

(defn register!
  []
  (doseq [[opening f closing] tags]
    (if closing ; we can’t use apply because add-tag! is a macro
      (sp/add-tag! opening f closing)
      (sp/add-tag! opening f))))
