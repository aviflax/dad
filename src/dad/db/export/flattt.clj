(ns dad.db.export.flattt
  "flattt = “Flatten To Tables” — I’m thinking of extracting this as a standalone library/tool.
  
  Prior art: https://github.com/OpenDataServices/flatten-tool"
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.walk :as walk :refer [keywordize-keys postwalk]]
            [dad.medley :as dm]
            [inflections.core :refer [singular]]
            [medley.core :as mc :refer [map-keys map-vals]]))

(s/def ::non-blank-string (s/and string? (complement str/blank?)))

(defn scalar?
  [v]
  (or (number? v) (string? v) (keyword? v)))

(s/def ::scalar scalar?)

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

(defn- unkeyword
  [v]
  (if (keyword? v)
    (name v)
    v))

(def ^:private separator "-")

(defn- join-names
  [names]
  (try
    (->> (map unkeyword names)
         (str/join separator)
         (keyword))
    (catch Exception e
      (println names)
      (throw e))))

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

(defn- kp->tp
  [kp]
  (cond
    (> (count kp) 2)
    [(first kp) (second kp) (join-names (drop 2 kp))]
    
    :else
    kp))

(defn- pathize
  {:derived-from "https://andersmurphy.com/2019/11/30/clojure-flattening-key-paths.html"}
  ([m]
   (pathize m []))
  ([m path]
   ; (println "map:" m "\npath:" path "\n\n")
   (->> m
       (map (fn [[k v]]
                 ; (println "key:" k "\n" "val:" v "\n\n\n")
                 (cond
                   (and (map? v) (seq v))  (pathize v (conj path k))
                   ; (and (not (map? v))
                   ;      (coll? v)
                   ;      (map? (first v)))  (->> (map-indexed (fn [i v] (pathize v (conj path (join-names [k i])))) v)
                   ;                              (apply concat))
                   :else                   [(conj path k) v])))
        (into {}))))

(defn- interpolate-paths
  [paths]
  (reduce-kv
    (fn [paths path _v]
      (let [path-variants (map #(take % path) (range 2 (count path) 2))]
         (->> (mapv #(vector % (last %)) path-variants)
              (into {})
              (merge paths))))
    paths
    paths))

(defn- key-col-name
  [key-val]
  (if (or (number? key-val)
          (uuid? key-val))
      :id
      :name))

(defn- path+value->rows
  [path v]
  (let [kp  path ;[:systems :Discourse :containers :web :technology]
        kpp (partition 2 kp)
        table  (-> (if (sequential? v)
                     (take-nth 2 kp)
                     (take-nth 2 (butlast kp)))
                   (join-names))
        p-keys (->> (butlast kpp)
                    (map (fn [[table-name key-val]] [(singular table-name) (unkeyword key-val)]))
                    (into {})
                    (merge (let [[_ key-val] (last kpp)]
                             {(key-col-name key-val) (unkeyword key-val)})))
        f-keys (->> (if (even? (count kp))
                      (butlast kpp)
                      kpp)
                    (map (fn [[table-name key-val]]
                              {:this-table-col (singular table-name)
                               :f-table-name   table-name
                               :f-table-col    (key-col-name key-val)})))
        col-name  (last kp)]
    ; (println "kp:    " kp)
    ; (println "kpp:   " kpp)
    ; (println "table: " table)
    ; (println "keys:  " keys)
    ; (println "col:   " col)
    (if (and (sequential? v)
             (map? (first v)))
      (map (fn [row]
             {:table-name table
              :p-keys [] ; p-keys
              :f-keys f-keys
              :cols row})
           v)
      [{:table-name table
        :p-keys p-keys
        :f-keys f-keys
        :cols {(if (odd? (count kp)) col-name :val) v}}])))

(defn- path+val->tables
  [tables [path v]]
  (let [kpp   (partition 2 path)
        table-name (->> (if (sequential? v)
                          path
                          (butlast path))
                   (take-nth 2)
                   (join-names))
        p-keys (->> (butlast kpp)
                    (map (fn [[table-name key-val]] [(singular table-name) (unkeyword key-val)]))
                    (into {})
                    (merge (let [[_ key-val] (last kpp)]
                             {(key-col-name key-val) (unkeyword key-val)})))
        col-name  (if (odd? (count path))
                    (last path)
                    :val)
        col (if (= (last path) v)
              {}
              {col-name v})]
    (update-in tables [table-name p-keys] merge col)))

(path+val->tables
  {}
  [[:systems :Discourse] :Discourse])

(defn db->tables
  [m]
  (->> m
       (keywordize-keys)
       (fold-props)
       (pathize)
       (interpolate-paths)
       (reduce path+val->tables {})))

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



#_(comment
  [:technologies :Kafka :recommendations 0 :type]

  (path+value->cell [:systems :Discourse :summary] "Web forums that don’t suck.")
  (path+value->cell [:systems :Discourse :containers :web :technology] "Tomcat")
  (path+value->cell [:systems :Discourse :containers :web :tags :regions] ["us", "uk"])
  (path+value->cell [:technologies :Clojure :recommendations 0 :type] "assess")
  
  
  (->> {:systems {:Discourse {:summary    "Web forums that don’t suck."
                              :links      {:main "https://discourse.org/"}
                              :containers {:web   {:summary "web server" :technology "Tomcat"}
                                           :db    {:summary "db server"  :technology "Access"}
                                           :cache {:summary "hot keys"   :technology "PHP"}}}}}
       (db->tables))

  (->> {:technologies {:Clojure {:links {:main "https://clojure.org/"}
                                 :recommendations [{:type "assess" :date "2011-09-15"}
                                                   {:type "adopt"  :date "2012-01-12"}]}}}
       (db->tables))
       ; (pathize) (partition 2)
       ; clojure.pprint/pprint)

  (->> {:technologies {:Clojure {:links {:main "https://clojure.org/"}
                                 :recommendations [{:type "assess" :date "2011-09-15"}
                                                   {:type "adopt"  :date "2012-01-12"}]}}}
       (db->tables))


  ; (->>
  ;   [[:systems :Discourse :summary]
  ;    [:systems :Discourse :links :main]
  ;    [:systems :Discourse :containers :web :summary]
  ;    [:systems :Discourse :containers :web :technology]]
  ;   (map kp->tp)
  ;   clojure.pprint/pprint)

  #_(
    [:systems :Discourse :containers :web :technology]
    ->
    [:systems-containers [:system :Discourse :name :web] :technology])


(->> [:technologies :Clojure :recommendations] (partition 2))

)
