(ns dad.rendering.filters
  "These filters were part of this codebase when it was just a subtree in a larger private internal
  repo within Funding Circle; they’re included here for now because for now we don’t have a better
  place to keep them. We should probably remove them from this project soon, because they’re highly
  specific to the needs of the Architecture team at Funding Circle, and this project is now intended
  to be useable/useful for people in other contexts outside of Funding Circle."
  (:require [clojure.string :as str :refer [lower-case]]
            [medley.core :as mc]
            [selmer.filters :as filters])
  (:import [java.time LocalDate ZonedDateTime]))

(defn- flexi-get
  ([coll k]
   (flexi-get coll k nil))
  ([coll k d]
   (or (get coll k)
       (get coll (name k))
       (get coll (lower-case (name k)))
       (get coll (keyword (lower-case (name k))))
       d)))

(defn- flex=
  [& vs]
  (apply = (map (comp lower-case name) vs)))

(def ^:private to-pattern (memoize re-pattern))

(def filters
  ;; TODO: some of these names are iffy. Rethink!
  {:filter-by (fn [m k v]
                (->> m
                     (mc/filter-vals (fn [im] (flex= (flexi-get im k) v)))
                     (into (empty m))))
   :get flexi-get

   :includes? str/includes?

   :join (fn [coll separator] (str/join separator coll))

   ; Given a sequence of maps, sort them by the specified date/time property, then return the map
   ; with the most recent date value in that property. The values must be either ISO-8601 dates
   ; (2020-04-20) or (COMING SOON) ISO-8601 date-times (2020-04-20T16:20:00-04:00).
   ; TODO: what should this do if the value isn’t a sequence of maps? Or if one or more of the maps
   ;       don’t have the specified property (key)?
   :latest-by (fn [maps k]
                (last (sort-by (fn [m] (LocalDate/parse (get m k))) maps)))

   ;; This needs to produce the same slugs that GitHub generates when rendering Markdown into HTML.
   ;; (GH generates “permalinks” for every Markdown header; the anchor is a slugified version of
   ;; the header text.)
   ;; We should probably replace this sketchy impl with one from a third-party library.
   :slugify #(some-> %
                     (name)
                     (lower-case)
                     (str/replace #"\s" "-")
                     (str/replace #"[^-_A-Za-z0-9]" ""))

   :parse-date-time #(try (when-not (str/blank? %) (ZonedDateTime/parse %))
                          (catch Exception _e nil))

   ; This name isn’t clear. It’s really sort-by-nested-key as it assumes that the input is a coll
   ; of maps. Gotta figure this out.
   :sort-by-key (fn [coll k]
                  (into (empty coll)
                        (sort-by (fn [[_k v]] (lower-case (flexi-get v k)))
                                 coll)))
   :sort-by-keys #(into (empty %)
                        (sort-by (fn [[k _v]] (lower-case (name k))) %))
   :sort-by-names (fn [coll]
                    (sort-by (fn [[k v]]
                               (lower-case (flexi-get v "full-name" (name k))))
                             coll))
   :split (fn [s regex] (str/split s (to-pattern regex)))
   :without (fn [coll k]
              (remove (fn [[_name props]]
                        (or (contains? props k)
                            (contains? props (keyword k))))
                      coll))})

(defn register!
  []
  (doseq [[n f] filters]
    (filters/add-filter! n f)))
