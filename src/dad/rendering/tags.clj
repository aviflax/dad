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

(defn- collapse-blank-lines-fn
  [tag-name]
  (fn [_args _context content]
    (str/replace (get-in content [tag-name :content]) #"\n{2,}" "\n")))

(defn- exec
  [args _context]
  (try (let [{:keys [exit out err] :as _res} (apply shell/sh (map unwrap args))
             err-msg "Command » %s « failed:\nexit code: %s\nstdout: %s\nstderr: %s\n\n"]
         (if (zero? exit)
           out
           (format err-msg (str/join " " args) exit out err)))
       (catch IOException e
         (format "Command » %s « failed: %s" (str/join " " args) e))))

(defn- replace-fn
  "Returns a custom tag function that’ll take 2 args: this and that, as in “replace this with that”.
  The first arg (“this”) will be interpreted as a regex. You may use double quotes to wrap the args,
  e.g. if you want to replace something with nothing: {% replace foo \"\" %}foo{% endreplace %}
  
  NB: as usual, the regex will be case-sensitive by default."
  [tag-name]
  (fn [[this that :as _args] _context content]
    (if (and this that)
      (str/replace (get-in content [tag-name :content])
                   (re-pattern (unwrap this))
                   (unwrap that))
      (str "ERROR: The DaD tag `replace` requires two args: `this` and `that` as in"
           " “replace this with that”."))))

(def tags
  [[:removeblanklines (collapse-blank-lines-fn :removeblanklines) :endremoveblanklines]
   [:replace (replace-fn :replace) :endreplace]
   [:exec exec]])

(defn register!
  []
  (doseq [[opening f closing] tags]
    (if closing ; we can’t use apply because add-tag! is a macro
      (sp/add-tag! opening f closing)
      (sp/add-tag! opening f))))
