(ns dad.rendering.filters
  "These filters were part of this codebase when it was just a subtree in a larger private internal
  repo within Funding Circle; they’re included here for now because for now we don’t have a better
  place to keep them. We should probably remove them from this project soon, because they’re highly
  specific to the needs of the Architecture team at Funding Circle, and this project is now intended
  to be useable/useful for people in other contexts outside of Funding Circle."
  (:require [clojure.string :as str :refer [blank? lower-case split]]
            [medley.core :as mc :refer [filter-vals]]
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

   ;; Given a coll of maps, sort them by the specified date/time property (as specified by a
   ;; dot-separated key path, to support nested maps) then return the map with the most recent date
   ;; value in that property. The property specifier may include `.` chars to refer to nested maps.
   ;; The values must be either ISO-8601 dates (2020-04-20) or (COMING SOON) ISO-8601 date-times
   ;; (2020-04-20T16:20:00-04:00).
   ;; If key-path is blank or not a string, throws.
   ;; If the specified key-path is not found in any of the maps, throws.
   ;; If any of the values at the specified key-path cannot be parsed into an ISO-8601 date, throws.
   ;; If the coll of maps is empty, returns the empty coll.
   ;; If the coll of maps contains a single value, skips parsing and returns the coll.
   ; TODO: what should this do if the value isn’t a coll of maps?
   :latest-by (fn [maps key-path]
                {:pre [(coll? maps)
                       (string? key-path)
                       (not (blank? key-path))]}
                (if (<= (count maps) 1)
                  maps
                  (let [kp (split key-path #"\.")]
                    (last (sort-by #(LocalDate/parse (get-in % kp))
                                   maps)))))

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
   
   ;; Given a map with values that are maps, or a non-associative coll of maps, and a key, return
   ;; the same coll but containing only those entries whose value contains the specified key.
   :with (fn [coll k]
           {:pre [(coll? coll)
                  (or (empty? coll)
                      (map? (first coll))
                      (and (map? coll)
                           (map? (val (first coll)))))
                  (string? k)
                  (not (blank? k))]}
           (let [f (if (map? coll) filter-vals filter)]
             (f #(or (contains? % k)
                     (contains? % (keyword k)))
                coll)))

   :without (fn [coll k]
              (remove (fn [[_name props]]
                        (or (contains? props k)
                            (contains? props (keyword k))))
                      coll))})

(defn register!
  []
  (doseq [[n f] filters]
    (filters/add-filter! n f)))
