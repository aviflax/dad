(ns dad.db.export.flattt-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer [deftest is are]]
            [dad.db.export.flattt :as f]
            [expound.alpha :as expound]
            [medley.core :as mc :refer [map-entry]])
  (:refer-clojure :exclude [*]))

; See https://github.com/bhb/expound#printer-options
(set! s/*explain-out* (expound/custom-printer {:print-specs? false}))

(stest/instrument (stest/enumerate-namespace 'dad.db.export.flattt))

(deftest add-fk
  (let [rec-m {:type "assess"
               :date "2011-09-15"}
        fk-table-name :technologies
        fk-table-key-val "Clojure"
        expected {:technology "Clojure"
                  :type "assess"
                  :date "2011-09-15"}
        expected-meta {::f/columns {:technology {::f/fk-table-name fk-table-name}}}
        res (#'f/add-fk rec-m fk-table-name fk-table-key-val)]
    (is (= expected res))
    (is (= expected-meta (meta res)))))

(deftest fold-props
  (are [in expected] (= expected (#'f/fold-props in))
  
    ; in
    {:containers  {:API     {:props {:marathon-ids {:kp "/saclib/api"}}}
                   :Hutch   {:props {:marathon-ids {:kp "/saclib/hutch"}
                                     :technologies ["RabbitMQ" "Ruby"]}}
                   :Sidekiq {:props {:marathon-ids {:kp "/saclib/sidekiq"}
                                     :technologies ["Ruby"]}}
                   :Web     {:props {:marathon-ids {:kp "/saclib/web"}}}}
     :description "Salad Container Library -- builds libraries of salad containers (duh)"
     :props       {:regions       ["kp"]
                   :marathon-ids  {:kp "/saclib"}
                   :repos         ["saclib"]
                   :related-repos ["saclib_adapter" "saclib-client"]}}
    ; expected
    {:containers   {:API     {:marathon-ids {:kp "/saclib/api"}}
                    :Hutch   {:marathon-ids {:kp "/saclib/hutch"}
                              :technologies ["RabbitMQ" "Ruby"]}
                    :Sidekiq {:marathon-ids {:kp "/saclib/sidekiq"}
                              :technologies ["Ruby"]}
                    :Web     {:marathon-ids {:kp "/saclib/web"}}}
     :description   "Salad Container Library -- builds libraries of salad containers (duh)"
     :regions       ["kp"]
     :marathon-ids  {:kp "/saclib"}
     :repos         ["saclib"]
     :related-repos ["saclib_adapter" "saclib-client"]}))

; (deftest flatten-paths
;   (are [in expected] (= expected (#'f/flatten-paths in "-"))
;
;     ; in
;     {:containers    {:API     {:marathon-ids {:kp "/saclib/api"}}
;                      :Hutch   {:marathon-ids {:kp "/saclib/hutch"}
;                                :technologies ["RabbitMQ" "Ruby"]}
;                      :Sidekiq {:marathon-ids {:kp "/saclib/sidekiq"}
;                                :technologies ["Ruby"]}
;                      :Web     {:marathon-ids {:kp "/saclib/web"}}}
;      :description   "Salad Container Library -- builds libraries of salad containers (duh)"
;      :regions       ["kp"]
;      :marathon-ids  {:kp "/saclib"}
;      :repos         ["saclib"]
;      :related-repos ["saclib_adapter" "saclib-client"]}
;
;     ; expected
;     {:containers      {:API     {:marathon-ids-kp "/saclib/api"}
;                        :Hutch   {:marathon-ids-kp "/saclib/hutch"
;                                  :technologies    ["RabbitMQ" "Ruby"]}
;                        :Sidekiq {:marathon-ids-kp "/saclib/sidekiq"
;                                  :technologies    ["Ruby"]}
;                        :Web     {:marathon-ids-kp "/saclib/web"}}
;      :description     "Salad Container Library -- builds libraries of salad containers (duh)"
;      :regions         ["kp"]
;      :marathon-ids-kp "/saclib"
;      :repos           ["saclib"]
;      :related-repos   ["saclib_adapter" "saclib-client"]}))
;
; (deftest split-record
;   (are [table-name record expected] (= expected (#'f/split-record table-name record))
;     ; table-name
;     :technologies
;     ; record
;     (map-entry "Clojure" {:links-main "https://clojure.org"
;                           :recommendations [{:type "assess" :date "2011-09-15"}
;                                             {:type "adopt"  :date "2012-01-12"}]})
;     ; expected
;     {:technologies                 {{:name "Clojure"} {:links-main "https://clojure.org"}}
;      :technologies-recommendations [{:technology "Clojure" :type "assess" :date "2011-09-15"}
;                                     {:technology "Clojure" :type "adopt"  :date "2012-01-12"}]}
;
;     ; --------------------
;
;     ; table-name
;     :systems
;     ; record
;     (map-entry "Discourse" {:links-main "https://discourse.org"
;                             :containers {:web   {:summary "web server" :technology "Tomcat"}
;                                          :db    {:summary "db server"  :technology "Access"}
;                                          :cache {:summary "hot keys"   :technology "PHP"}}})
;     ; expected
;     {:systems            {{:name "Discourse"} {:links-main "https://discourse.org"}}
;      :systems-containers {{:system "Discourse" :name "web"}   {:summary "web server" :technology "Tomcat"}
;                           {:system "Discourse" :name "db"}    {:summary "db server"  :technology "Access"}
;                           {:system "Discourse" :name "cache"} {:summary "hot keys"   :technology "PHP"}}}
;
;     ; --------------------
;
;     ; table-name
;     :systems
;     ; record
;     (map-entry "SACLIB" {:containers      {:API     {:marathon-ids-kp "/saclib/api"}
;                                            :Hutch   {:marathon-ids-kp "/saclib/hutch"
;                                                      :technologies ["RabbitMQ" "Ruby"]}
;                                            :Sidekiq {:marathon-ids-kp "/saclib/sidekiq"
;                                                      :technologies ["Ruby"]}
;                                            :Web     {:marathon-ids-kp "/saclib/web"}}
;                          :description     "Salad Container Library -- builds libraries of salad containers (duh)"
;                          :regions         ["kp"]
;                          :marathon-ids-kp "/saclib"
;                          :repos           ["saclib"]
;                          :related-repos   ["saclib_adapter" "saclib-client"]})
;     ; expected
;     {:systems            {{:name "SACLIB"} {:description     "Salad Container Library -- builds libraries of salad containers (duh)"
;                                             :regions         ["kp"]
;                                             :marathon-ids-kp "/saclib"
;                                             :repos           ["saclib"]
;                                             :related-repos   ["saclib_adapter" "saclib-client"]}}
;      :systems-containers {{:system "SACLIB" :name "API"}     {:marathon-ids-kp "/saclib/api"}
;                           {:system "SACLIB" :name "Hutch"}   {:marathon-ids-kp "/saclib/hutch"
;                                                              :technologies    ["RabbitMQ" "Ruby"]}
;                           {:system "SACLIB" :name "Sidekiq"} {:marathon-ids-kp "/saclib/sidekiq"
;                                                              :technologies    ["Ruby"]}
;                           {:system "SACLIB" :name "Web"}     {:marathon-ids-kp "/saclib/web"}}})
;
;   (let [table-name :technologies
;         record (map-entry "Clojure" {:links-main "https://clojure.org"
;                                      :recommendations [{:type "assess" :date "2011-09-15"}
;                                                        {:type "adopt"  :date "2012-01-12"}]})
;         res (#'f/split-record table-name record)]
;     (is (s/valid? ::f/tables res) (s/explain-str ::f/tables res))))





(deftest pathize
  (are [in expected] (= expected (#'f/pathize in))
    ; in
    {:technologies {:Clojure {:links {:main "https://clojure.org"}
                              :recommendations [{:type "assess" :date "2011-09-15"}
                                                {:type "adopt"  :date "2012-01-12"}]}}}
    
    ; expected
    {[:technologies :Clojure :links :main]     "https://clojure.org"
     [:technologies :Clojure :recommendations] [{:type "assess" :date "2011-09-15"}
                                                {:type "adopt"  :date "2012-01-12"}]}
  
    ; --------------------

    ; in
    {:systems {:Discourse {:summary    "Web forums that don’t suck."
                           :links      {:main "https://discourse.org"}
                           :containers {:web   {:summary "web server" :technology "JRun" :tags {:regions ["us", "uk"]}} :db    {:summary "db server"  :technology "Access"}}}}}
    ; expected
    {[:systems :Discourse :summary]                        "Web forums that don’t suck."
     [:systems :Discourse :links :main]                    "https://discourse.org"
     [:systems :Discourse :containers :web :summary]       "web server"
     [:systems :Discourse :containers :web :technology]    "JRun"
     [:systems :Discourse :containers :web :tags :regions] ["us", "uk"]
     [:systems :Discourse :containers :db :summary]        "db server"
     [:systems :Discourse :containers :db :technology]     "Access"}))

(def *
  (proxy [Object] [] (equals [o] true)))

(deftest path+value->rows
  (are [path v expected] (= expected (#'f/path+value->rows path v))
    [:technologies :Clojure :recommendations]
    [{:type "assess" :date "2011-09-15"}
     {:type "adopt"  :date "2012-01-12"}]
    [{:table-name :technologies-recommendations
      :p-keys     {:id *}
      :f-keys     {:technology "Clojure"}
      :cols       {:type "assess" :date "2011-09-15"}}
     {:table-name :technologies-recommendations
       :p-keys     {:id *}
       :f-keys     {:technology "Clojure"}
       :cols       {:type "adopt"  :date "2012-01-12"}}]

    [:systems :Discourse :summary]
    "Web forums that don’t suck."
    [{:table-name :systems
      :p-keys     {:name "Discourse"}
      :f-keys     {}
      :col-name   :summary
      :val        "Web forums that don’t suck."}]
  
    [:systems :Discourse :links :main]
    "https://discourse.org"
    [{:table-name :systems-links
      :p-keys     {:name "main" :system "Discourse"}
      :f-keys     [{:this-table-col :system
                    :f-table-name   :systems
                    :f-table-col    :name}]
      :col-name   :val
      :val        "https://discourse.org"}]
  
    [:systems :Discourse :containers :web :technology]
    "Tomcat"
    [{:table-name :systems-containers
      :p-keys     {:name "web" :system "Discourse"}
      :f-keys     [{:this-table-col :system
                    :f-table-name   :systems
                    :f-table-col    :name}]
      :col-name   :technology
      :val        "Tomcat"}]
    
    [:systems :Discourse :containers :web :tags :regions]
    ["us", "uk"]
    [{:table-name :systems-containers-tags
      :p-keys     {:system "Discourse" :container "web" :name "regions"}
      :f-keys     [{:this-table-col :system
                    :f-table-name   :systems
                    :f-table-col    :name}
                   {:this-table-col :container
                    :f-table-name   :containers
                    :f-table-col    :name}]
      :col-name   :val
      :val        ["us", "uk"]}]
    
    [:technologies :Clojure :recommendations 0 :type]
    "assess"
    [{:table-name :technologies-recommendations
      :p-keys     {:technology "Clojure" :id 0}
      :f-keys     [{:this-table-col :technology
                    :f-table-name   :technologies
                    :f-table-col    :name}]
      :col-name   :type
      :val        "assess"}]))

(deftest db->tables
  (let [db {:technologies {:Clojure {:links           {:main "https://clojure.org"}
                                     :props           {:hosted "true"}
                                     :recommendations [{:type "assess" :date "2011-09-15"}
                                                       {:type "adopt" :date "2012-01-12"}]}
                           :Crux    {:links           {:main "https://opencrux.com"}}
                           :Kafka   {:links           {:main "https://kafka.apache.org"}
                                     :recommendations [{:type "assess" :date "2013-12-16"}
                                                       {:type "adopt"  :date "2016-03-03"}]}}
            :systems      {:Discourse {:links      {:main "https://discourse.org"}
                                       :containers {:web   {:summary "web server" :technology "Tomcat"}
                                                    :db    {:summary "db server"  :technology "Access"}
                                                    :cache {:summary "hot keys"   :technology "PHP"}}}}}
        expected {:technologies                 {{:name "Clojure"} {:hosted "true"}
                                                 {:name "Crux"}    {}
                                                 {:name "Kafka"}   {}}
                  :technologies-links           {{:name "main" :technology "Clojure"} {:val "https://clojure.org"}
                                                 {:name "main" :technology "Crux"}    {:val "https://opencrux.com"}
                                                 {:name "main" :technology "Kafka"}   {:val "https://kafka.apache.org"}}
                  :technologies-recommendations {{:id 0 :technology "Clojure"} {:type "assess" :date "2011-09-15"}
                                                 {:id 1 :technology "Clojure"} {:type "adopt"  :date "2012-01-12"}
                                                 {:id 0 :technology "Kafka"}   {:type "assess" :date "2013-12-16"}
                                                 {:id 1 :technology "Kafka"}   {:type "adopt"  :date "2016-03-03"}}
                  :systems                      {{:name "Discourse"} {}}
                  :systems-links                {{:name "main" :system "Discourse"} {:val "https://discourse.org"}}
                  :systems-containers           {{:system "Discourse" :name "web"}   {:summary "web server" :technology "Tomcat"}
                                                 {:system "Discourse" :name "db"}    {:summary "db server"  :technology "Access"}
                                                 {:system "Discourse" :name "cache"} {:summary "hot keys"   :technology "PHP"}}}
        res (#'f/db->tables db)]
    (is (= expected res))
    (is (s/valid? ::f/tables res) (s/explain-str ::f/tables res))
    (doseq [row (:technologies-recommendations res)]
      (is (= {::f/columns {:technology {::f/fk-table-name :technologies}}}
             (meta row))))))
