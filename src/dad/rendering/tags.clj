(ns dad.rendering.tags
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [selmer.parser :as sp]))

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
  "If the program named in the first arg doesn’t exist, an IOException will be thrown. If it exists,
  but exits with a non-zero exit code, a clojure.lang.ExceptionInfo will be thrown with its data
  map containing the keys :stdout and :stderr."
  [args _context]
  (let [{:keys [exit out err] :as _res} (apply shell/sh (map unwrap args))]
    (if (zero? exit)
      out
      (throw (ex-info (format "Command » %s « failed with exit code %s" (str/join " " args) exit)
                      {:stdout out :stderr err})))))

(def tags
  [remove-blank-lines
   [:exec exec]])

(defn register!
  []
  (doseq [[opening f closing] tags]
    (if closing ; we can’t use apply because add-tag! is a macro
      (sp/add-tag! opening f closing)
      (sp/add-tag! opening f))))
