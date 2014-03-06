(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [datomic.api :as d]
            [datomic-schema.schema :as dsa]
            [datomic-schematode.core :as ds-core]
            [datomic-schematode.core-test :as ds-test]))

(def db-url "datomic:mem://testdb")

(defn start!
  "Starts the current development system."
  []
  (d/create-database db-url)
  (map (partial d/transact (d/connect db-url))
       (ds-core/schematize ds-test/test-schemas d/tempid)))

(defn stop!
  "Shuts down and destroys the current development system."
  []
  (d/delete-database db-url))

(defn reset []
  (stop!)
  (refresh :after 'user/start!))

(defn touch-that
  "Execute the specified query on the current DB and return the results of touching each entity.
   The first binding must be to the entity.
   All other bindings are ignored."
  [query]
  (map #(d/touch
         (d/entity
          (d/db (d/connect db-url))
          (first %)))
       (d/q query
            (d/db (d/connect db-url)))))

(defn ptouch-that
  "Example: (ptouch-that '[:find ?e :where [?e :user/username]])"
  [query]
  (pprint (touch-that query)))

(comment
  (d/transact (d/connect db-url) [{:db/id #db/id[:db.part/user] :user/username "fleem"}])
  (d/transact (d/connect db-url) [{:db/id #db/id[:db.part/user] :user/username "fleem2" :user/pwd "hash123"}])

  ;; in datomic, always think: EAVT (Entity, Attribute, Value, Time)
  (def b1 (d/q '[:find ?e
                 :where [?e :user/username]]
               (d/db (d/connect db-url))))
  (map #(d/touch
         (d/entity
          (d/db (d/connect db-url))
          (first %)))
       (d/q '[:find ?e
              :where [?e :db/ident]]
            (d/db (d/connect db-url))))

  
  (d/transact (d/connect db-url)
              [{:db/id (d/tempid :db.part/user)
                :db/ident :hello2
                :db/fn (d/function '{:lang :clojure
                                     :params [name]
                                     :code (str "Hello, " name)})}])

  (def dbval (d/db (d/connect db-url)))
  ;; retrieve function from db and call it
  (def hello-from-db (d/entity dbval :hello))
  (:db/doc hello-from-db)
  ((:db/fn hello-from-db) "John")

  ;;add a transaction function called add-doc
  (d/transact (d/connect db-url)
              [{:db/id #db/id [:db.part/user]
                :db/ident :add-doc
                :db/fn (d/function '{:lang "java"
                                     :params [db e doc]
                                     :code "return list(list(\":db/add\", e, \":db/doc\", doc));"})}])

  (d/transact (d/connect db-url)
              [{:db/id #db/id [:db.part/user] :db/ident :foo}])

  (d/transact (d/connect db-url)
              [[:add-doc :foo "this is foo's doc"]])

  (d/touch (d/entity
            (d/db (d/connect db-url))
            :foo))

  (d/transact (d/connect db-url)
              [{:db/id #db/id [:db.part/user]
                :db/ident :add-user
                :db/fn (d/function '{:lang "clojure"
                                     :params [db u]
                                     :code
                                     (if (:user/username u)
                                       [(merge u {:db/id #db/id [:db.part/user]})]
                                       (throw (Exception. "Bad username. Bad!")))})}])

  (d/transact (d/connect db-url)
              [{:db/id #db/id [:db.part/user]
                :db/ident :add-user
                :db/fn (d/function '{:lang "clojure"
                                     :params [db u]
                                     :code
                                     (if (:user/username u)
                                       [(merge u {:db/id #db/id [:db.part/user]})]
                                       (throw (Exception. "Bad username. Bad!")))})}])

  (d/transact (d/connect db-url)
              [{:db/id #db/id [:db.part/user] :db/ident :foo8}])

  (d/transact (d/connect db-url)
              [[:add-user :foo8 "this is fooboo's doc"]])

  (d/touch (d/entity
            (d/db (d/connect db-url))
            :foo8))

  (d/entity (d/db (d/connect db-url)) :foo8)

  (def datomic-conn (d/connect db-url))
  (def db (d/db datomic-conn))
  (def user (d/entity (d/db datomic-conn) :add-user))

  ;; http://dbs-are-fn.com/2013/datomic_history_of_an_entity/
  (->>
   ;; This query finds all transactions that touched a particular entity
   (d/q
    '[:find ?tx
      :in $ ?e
      :where
      [?e _ _ ?tx]]
    (d/history (d/db datomic-conn))
    (:db/id user))
   ;; The transactions are themselves represented as entities. We get the
   ;; full tx entities from the tx entity IDs we got in the query.
   (map #(d/entity (d/db datomic-conn) (first %)))
   ;; The transaction entity has a txInstant attribute, which is a timestmap
   ;; as of the transaction write.
   (sort-by :db/txInstant)
   ;; as-of yields the database as of a t. We pass in the transaction t for
   ;; after, and (dec transaction-t) for before. The list of t's might have
   ;; gaps, but if you specify a t that doesn't exist, Datomic will round down.
   (map
    (fn [tx]
      {:before (d/entity (d/as-of db (dec (d/tx->t (:db/id tx)))) (:db/id user))
       :after (d/entity (d/as-of db (:db/id tx)) (:db/id user))})))



  ;;;; http://dbs-are-fn.com/2013/datomic_history_of_an_entity/
  ;;;; part 2
  (->>
   ;; This query finds all tuples of the tx and the actual attribute that
   ;; changed for a specific entity.
   (d/q
    '[:find ?tx ?a
      :in $ ?e
      :where
      [?e ?a _ ?tx]]
    (d/history db)
    (:db/id user))
   ;; We group the tuples by tx - a single tx can and will contain multiple
   ;; attribute changes.
   (group-by (fn [[tx attr]] tx))
   ;; We only want the actual changes
   (vals)
   ;; Sort with oldest first
   (sort-by (fn [[tx attr]] tx))
   ;; Creates a list of maps like '({:the-attribute {:old "Old value" :new "New value"}})
   (map
    (fn [changes]
      {:changes (into
                 {}
                 (map
                  (fn [[tx attr]]
                    (let [tx-before-db (d/as-of db (dec (d/tx->t tx)))
                          tx-after-db (d/as-of db tx)
                          tx-e (d/entity tx-after-db tx)
                          attr-e-before (d/entity tx-before-db attr)
                          attr-e-after (d/entity tx-after-db attr)]
                      [(:db/ident attr-e-after)
                       {:old (get
                              (d/entity tx-before-db (:db/id user))
                              (:db/ident attr-e-before))
                        :new (get
                              (d/entity tx-after-db (:db/id user))
                              (:db/ident attr-e-after))}]))
                  changes))
       :timestamp (->> (ffirst changes)
                       (d/entity (d/as-of db (ffirst changes)))
                       :db/txInstant)})))
  

  (d/transact (d/connect db-url)
              [{:db/id #db/id[:db.part/user]
                :user/username "fleem2"
                :user/pwd "password 2secrets!"
                :user/email "some2@where.com"
                :db/fn (d/function '{:lang :clojure
                                     :params [name]
                                     :code (str "Hello, " name)})}])

  (d/transact (d/connect db-url)
              [{:db/id #db/id [:db.part/user]
                :user/username "bob"}])

  (d/touch (d/entity (d/db (d/connect db-url))
                     (ffirst (d/q '[:find ?e :where [?e :user/username "fleem2"]]
                                  (d/db (d/connect db-url))))))

  (def fsharp (d/entity (d/db datomic-conn) 17592186045433))
  ((:db/fn fsharp) "yeep")
  
  )
