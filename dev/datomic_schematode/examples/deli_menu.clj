(ns datomic-schematode.examples.deli-menu
  (:require [clojure.pprint :refer [pprint]]
            [datomic.api :as d]
            [datomic-schematode.core :as ds-core]
            [datomic-schematode.constraints :as ds-constraints]))

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


;; Demonstrate Constraint Features
;; -------------------
(def schema2
  [[:sandwich {:attrs [[:name :string :indexed]
                       [:bread :enum [:focaccia :wheat :maize :rice] :indexed]
                       [:meat :string "Many people like meat on their sandwiches"]
                       [:needs-toothpick :boolean]]
               :dbfns [;; We can express any db/fns we want here. If a
                       ;; db/fn has the
                       ;; :schematode-constraint-fn/active attribute
                       ;; with the value true, it will be called as a
                       ;; schematode constraint fn, which must return
                       ;; either nil (success) or a message explaining
                       ;; the violated constraint.
                       {:db/ident :my-fn ; The :ident will be namespaced! ...in this case, to :sandwich/my-fn
                        :schematode-constraint-fn/active true ; required
                        :schematode-constraint-fn/name "Avoid at least one gross sandwich name" ; optional
                        :schematode-constraint-fn/desc "Sandwiches with gross names repel customers." ; optional
                        :db/fn (d/function '{:lang :clojure
                                             :params [db]
                                             :code (if (empty? (d/q '[:find ?e
                                                                      :where [?e :sandwich/name "soap-scum"]]
                                                                    db))
                                                     nil
                                                     "Ew. You are not allowed to name a sandwich \"soap-scum\".")})}
                       ;; We can use helper fns to create common constraints.
                       (ds-constraints/unique :sandwich :bread :meat)]}]
   [:salad {:attrs [[:name :string :indexed]
                    [:base :enum [:lettuce :spinach :pasta :unicorns] :indexed]
                    [:dressing :enum [:ranch :honey-mustard :italian :ceasar :minoan]]]}]])

(defn step4
  "You must init-schematode-constraints! before you can use
   schematode's constraint features."
  []
  (ds-core/init-schematode-constraints! db-conn))

(defn step5
  "schema2 contains db/fns with :schematode-constraint-fn attrs."
  []
  (ds-core/load-schema! db-conn schema2))

(defn step6
  "Can we violate our constraints?"
  []
  (d/transact db-conn
              [[:schematode-tx :enforce [{:db/id (d/tempid :db.part/user)
                                          :sandwich/name "soap-scum"}
                                         {:db/id (d/tempid :db.part/user)
                                          :sandwich/name "Just Rice"
                                          :sandwich/bread :sandwich.bread/rice
                                          :sandwich/meat ""}
                                         {:db/id (d/tempid :db.part/user)
                                          :sandwich/name "Only Rice"
                                          :sandwich/bread :sandwich.bread/rice
                                          :sandwich/meat ""}]]]))

(defn step6
  "Can we violate our constraints?"
  []
  (d/transact db-conn
              [[:schematode-tx :enforce [{:db/id (d/tempid :db.part/user)
                                          :sandwich/name "soap-scum"}
                                         {:db/id (d/tempid :db.part/user)
                                          :sandwich/name "Just Rice"
                                          :sandwich/bread :sandwich.bread/rice
                                          :sandwich/meat ""}
                                         {:db/id (d/tempid :db.part/user)
                                          :sandwich/name "Only Rice"
                                          :sandwich/bread :sandwich.bread/rice
                                          :sandwich/meat ""}]]]))
(defn step7
  "Test our constraints without attempting to transact anything."
  []
  (let [my-schematode-tx* (:db/fn (d/entity (d/db db-conn) :schematode-tx*))]
    (my-schematode-tx* (d/db db-conn)
                       [{:db/id (d/tempid :db.part/user)
                         :sandwich/name "soap-scum"}
                        {:db/id (d/tempid :db.part/user)
                         :sandwich/name "Just Rice"
                         :sandwich/bread :sandwich.bread/rice
                         :sandwich/meat ""}
                        {:db/id (d/tempid :db.part/user)
                         :sandwich/name "Only Rice"
                         :sandwich/bread :sandwich.bread/rice
                         :sandwich/meat ""}])))

(defn step8
  "Apply constraint warning messages to the TX entity, but let the
   transaction go through.

   N.B.: This is not good practice in production, because any
   violations that are not cleaned up will continue to attach
   violation messages to every TX entity."
  []
  (d/transact db-conn
              [[:schematode-tx :warn [{:db/id (d/tempid :db.part/user)
                                       :sandwich/name "soap-scum"}
                                      {:db/id (d/tempid :db.part/user)
                                       :sandwich/name "Just Rice"
                                       :sandwich/bread :sandwich.bread/rice
                                       :sandwich/meat ""}
                                      {:db/id (d/tempid :db.part/user)
                                       :sandwich/name "Only Rice"
                                       :sandwich/bread :sandwich.bread/rice
                                       :sandwich/meat ""}]]]))

(defn step9
  "What is the performance cost of using :schematode-tx?"
  []
  (let [db (d/db db-conn)
        query '[:find ?e :where [?e :schematode-constraint/elapsed-msec]]]
    (map #(:schematode-constraint/elapsed-msec (d/entity db (first %)))
         (d/q query db))))
