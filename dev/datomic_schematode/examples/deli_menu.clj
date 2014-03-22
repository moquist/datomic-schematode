(ns datomic-schematode.examples.deli-menu
  (:require [clojure.pprint :refer [pprint]]
            [datomic.api :as d]
            [datomic-schematode.core :as ds-core]))

(def db-url "datomic:mem://menudb")
(d/create-database db-url)
(def db-conn (d/connect db-url))

(def schema1
  [[:sandwich {:attrs [[:bread-name :string :indexed]
                       [:meat :string "Many people like meat on their sandwiches"]
                       [:needs-toothpick :boolean]]}]])

(defn step1 []
  (ds-core/load-schema! db-conn schema1))

(defn step2 []
  (d/transact db-conn
              [{:db/id #db/id[:db.part/user]
                :sandwich/bread-name "focaccia"
                :sandwich/meat "corned beef"
                :sandwich/needs-toothpick true}
               {:db/id #db/id[:db.part/user]
                :sandwich/bread-name "rye"
                :sandwich/meat "turky"
                :sandwich/needs-toothpick false}]))

(defn step3 []
  (let [db (d/db db-conn)
        entities (map #(d/touch
                        (d/entity db
                                  (first %)))
                      (d/q '[:find ?e
                             :where [?e :sandwich/bread-name]] db))]
    (pprint entities)
    (count entities)))


