(ns datomic-schematode.examples.deli-menu
  (:require [clojure.pprint :refer [pprint]]
            [datomic.api :as d]
            [datomic-schematode.core :as ds-core]))

(def db-url "datomic:mem://menudb")
(d/create-database db-url)
(def db-conn (d/connect db-url))

;; Demonstrate Basic Features
;; -------------------
(def schema1
  [[:sandwich {:attrs [[:name :string :indexed]
                       [:bread :enum [:focaccia :wheat :maize :rice] :indexed]
                       [:meat :string "Many people like meat on their sandwiches"]
                       [:needs-toothpick :boolean]]}]
   [:salad {:attrs [[:name :string :indexed]
                    [:base :enum [:lettuce :spinach :pasta :unicorns] :indexed]
                    [:dressing :enum [:ranch :honey-mustard :italian :ceasar :minoan]]]}]])

(defn step1 []
  (ds-core/load-schema! db-conn schema1))

(defn step2 []
  (d/transact db-conn
              [{:db/id #db/id[:db.part/user]
                :sandwich/name "Norville's #1"
                :sandwich/bread :sandwich.bread/focaccia
                :sandwich/meat "corned beef"
                :sandwich/needs-toothpick true}
               {:db/id #db/id[:db.part/user]
                :sandwich/name "Thanksgiving Leftovers"
                :sandwich/bread :sandwich.bread/maize
                :sandwich/meat "turkey"
                :sandwich/needs-toothpick false}
               {:db/id #db/id[:db.part/user]
                :salad/name "Ceasar"
                :salad/base :salad.base/lettuce
                :salad/dressing :salad.dressing/ceasar}]))

(defn step3 []
  (let [db (d/db db-conn)
        entities (map #(d/touch
                        (d/entity db
                                  (first %)))
                      (d/q '[:find ?e
                             :where [?e :sandwich/bread]] db))]
    (pprint entities)
    (count entities)))
