(ns dad.db.export.flattt
  "flattt = “Flatten To Tables” — I’m thinking of extracting this as a standalone library/tool.
  
  Prior art: https://github.com/OpenDataServices/flatten-tool"
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.walk :as walk :refer [postwalk]]
            [dad.medley :as dm]
            [inflections.core :refer [singular]]
            [medley.core :as mc :refer [map-keys map-vals]]))

(s/def ::non-blank-string (s/and string? (complement str/blank?)))
(s/def ::non-empty-scalar (s/or :number number?
                                :keyword keyword?
                                :string ::non-blank-string))
(s/def ::col-name         keyword?)
(s/def ::col-val          (s/or :scalar      ::non-empty-scalar
                                :scalar-coll (s/coll-of ::non-empty-scalar :gen-max 10)))
(s/def ::record-key-cols  (s/map-of ::col-name ::col-val :gen-max 10))
(s/def ::record-val-cols  (s/map-of ::col-name ::col-val :gen-max 10))
(s/def ::keyed-rows       (s/map-of ::record-key-cols ::record-val-cols :gen-max 10))
(s/def ::unkeyed-rows     (s/coll-of ::record-val-cols :gen-max 10))
(s/def ::table-name       keyword?)
(s/def ::tables           (s/map-of ::table-name
                                    (s/or :keyed ::keyed-rows
                                          :unkeyed ::unkeyed-rows)
                                    :gen-max 10))

(defn- join-names
  [separator names]
  (->> (map name names)
       (str/join separator)
       (keyword)))

;; This is used only for testing.
(s/def ::simple-test-map
  ;; Using every-kv and every here because they don’t conform, and conformance is both unneeded here
  ;; and also causes problems in the fdef:fn.
  (s/every-kv (s/or :s string? :k keyword?)
              (s/or :s ::non-empty-scalar
                    :sc (s/every ::non-empty-scalar :gen-max 5)
                    :scm (s/every ::simple-test-map :gen-max 2)
                    :m ::simple-test-map)
              :gen-max 5
              :conform-keys true))

(s/fdef fold-props
  :args (s/cat :m (s/with-gen ::simple-test-map
                    #(gen/frequency [[9 (s/gen ::simple-test-map)]
                                     [1 (gen/return {:foo {:bar "baz"
                                                           :props (gen/generate (s/gen ::simple-test-map))}})]])))
  :ret  ::simple-test-map
  :fn   (fn [{{in :m} :args
              out     :ret}]
          (let [map-seq (fn map-seq [m] (tree-seq map? #(interleave (keys %) (vals %)) m))]
            (if (some :props (map-seq in))
              ; TODO: this is incomplete; it doesn’t assert that the entries of :props are retained
              (not-any? :props (map-seq out))
              (= out in)))))

(defn- fold-props
  [m]
  (postwalk
    (fn [v]
      (if-let [props (and (map? v) (map? (:props v)) (:props v))]
        (merge props (dissoc v :props))
        v))
    m))

(defn- flatten-paths
  {:derived-from "https://andersmurphy.com/2019/11/30/clojure-flattening-key-paths.html"}
  ([m separator]
   (flatten-paths m separator []))
  ([m separator path]
   (->> (map (fn [[k v]]
               (if (and (map? v)
                        (not-empty v)
                        (not (map? (val (first v)))))
                 (flatten-paths v separator (conj path k))
                 [(->> (conj path k)
                       (join-names separator))
                  (if (and (map? v)
                           (some-> v first val map?))
                    (flatten-paths v separator)
                    v)]))
             m)
        (into {}))))

(defn- kp->tp
  [kp]
  (cond
    (> (count kp) 2)
    [(first kp) (second kp) (join-names "-" (drop 2 kp))]
    
    :else
    kp))

(defn- pathize
  {:derived-from "https://andersmurphy.com/2019/11/30/clojure-flattening-key-paths.html"}
  ([m]
   (pathize m []))
  ([m path]
   (mapcat (fn [[k v]]
             (if (and (map? v) (seq v))
               (pathize v (conj path k))
               [(conj path k) v]))
           m)))

(->> {:systems {:Discourse {:summary    "Web forums that don’t suck."
                            :links      {:main "https://discourse.org/"}
                            :containers {:web   {:summary "web server" :technology "Tomcat"}
                                         :db    {:summary "db server"  :technology "Access"}
                                         :cache {:summary "hot keys"   :technology "PHP"}}}}}
    (pathize)
    (partition 2)
    (reduce (fn [r [kp v]]
              (assoc-in r (kp->tp kp) v))
            {})
    clojure.pprint/pprint)

(->>
  [[:systems :Discourse :summary]
   [:systems :Discourse :links :main]
   [:systems :Discourse :containers :web :summary]
   [:systems :Discourse :containers :web :technology]]
  (map kp->tp)
  clojure.pprint/pprint)

#_(
[:systems :Discourse :containers :web :technology]
->
[:systems-containers [:system :Discourse :name :web] :technology]
)


(let [kp  [:systems :Discourse :containers :web :technology]
      kpp (partition 2 kp)
      table  (->> (map first kpp)
                  (join-names "-"))
      keys (-> (mapv vec kpp)
               (update-in [0 0] singular)
               (assoc-in [1 0] :name)
               (->> (into {})))
      col  (last kp)]
  ; (println "kp:    " kp)
  ; (println "kpp:   " kpp)
  ; (println "table: " table)
  ; (println "keys:  " keys)
  ; (println "col:   " col)
  {:table-name table
   :keys keys
   :col-name col})



(defn- ->key-col
  [k]
  (cond
    (map? k) k
    (or (string? k) (keyword? k)) {:name (name k)}))

(s/fdef add-fk
  :args (s/cat :val-cols         ::record-val-cols
               :fk-table-name    ::table-name
               :fk-table-key-val ::col-val)
  :ret  ::record-val-cols)

(defn- add-fk
  [rec-m fk-table-name fk-table-key-val]
  (let [col-name (singular fk-table-name)
        key-val (if (keyword? fk-table-key-val)
                  (name fk-table-key-val)
                  fk-table-key-val)]
    (-> (assoc rec-m col-name key-val)
        (with-meta {::columns {col-name {::fk-table-name fk-table-name}}}))))

(def separator "-")

(defn- split-record
  "Accepts a table name and a single record as either a MapEntry or a two-tuple.
  Returns a map of table name to maps representing records."
  [table-name [rec-key rec-m :as _record]]
  (reduce-kv
    (fn [r k v]
      (cond
        (and (coll? v) (map? v))
        (assoc r
              (join-names separator [table-name k])
              (map-keys #(-> (->key-col %)
                             (add-fk table-name rec-key)) v))
        
        (and (coll? v)
             (not (map? v))
             (map? (first v)))
        (assoc r
              (join-names separator [table-name k])
              (map #(add-fk % table-name rec-key) v))
        
        :else
        (assoc-in r [table-name (->key-col rec-key) k] v)))
    {}
    rec-m))

(defn recordset->tables
  "Transforms the recordset into one or more tables. The recordset should be either a MapEntry or a
  two-tuple. Returns a map."
  [[rs-name rs-recs :as _recordset]]
  (->> (walk/keywordize-keys rs-recs)
       (map-vals fold-props)
       (map-vals #(flatten-paths % separator))
       (map #(split-record rs-name %))
       (reduce dm/deep-mergecat)))

(defn flatten-db
  [db]
  (->> (dissoc db :schemata)
       (pmap recordset->tables)
       (reduce merge)))
