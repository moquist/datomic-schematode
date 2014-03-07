(ns datomic-schematode.core
  (:require [datomic.api :as d]
            [datomic-schema.schema :as dsa]))

(def constraints-schema
  [:schematode-constraint {:attrs [[:name :string :indexed]
                                   [:desc :string]]
                           :part :user}])

(def tx-fns
  ;; todo: add timing instrumentation and add attributes to the TX so we know the cost of schematode constraints
  [{:db/ident :schematode-tx
    :db/fn (d/function '{:lang :clojure
                         :doc "Applies all schematode constraints."
                         :params [db txs]
                         :code (let [wdb (:db-after (d/with db txs))
                                     constraints (map (fn schematode-tx1 [[c]]
                                                        (d/entity wdb c))
                                                      (d/q '[:find ?e
                                                             :where [?e :schematode-constraint/name]] wdb))
                                     msgs (if (empty? constraints)
                                            nil
                                            (map (fn schematode-tx2 [c] ((:db/fn c) wdb)) constraints))]
                                 (if (every? nil? msgs)
                                   txs
                                   ;; TODO: filter nils out
                                   (throw (Exception. (apply str msgs)))))})}])

(defn expand-fields
  "fn replacement for datomic-schema.schema/fields.
  Takes in any number of field vectors like [fname type & opts].
  Returns a map suitable for processing by
  datomic-schema.schema/field-to-datomic."
  [fdefs]
  (reduce (fn expand-fields- [a [fname type & opts]]
            (assoc a
              (name fname)
              [type (set opts)]))
          {}
          fdefs))

(defn chunk-schemas
  "Tranform a seq of :namespace,schema pairs into a seq of '(namespace schema) seqs"
  [sdefs]
  (partition 2 sdefs))

(defn expand-schemas
  "Transform a seq of :namespace,schema pairs into a seq of
  schema-expression maps suitable for processing by
  datomic-schema.schema/generate-schema."
  [sdefs]
  (reduce (fn expand-schemas- [a [sname sdef]]
            (let [part (keyword "db.part" (name (or (:part sdef) "user")))
                  sname (name sname)
                  attrs (expand-fields (:attrs sdef))]
              (conj a
                    {:part part
                     :namespace sname
                     :name sname
                     :basetype (keyword sname)
                     :fields attrs})))
          []
          (chunk-schemas sdefs)))

(defn partize
  "Transform a seq of :namespace,schema pairs into a seq of
  transactable partition-installation maps."
  [sdefs tempid-fn]
  (reduce (partial dsa/part-to-datomic tempid-fn)
          []
          (remove #{:db.part/user}
                  (distinct
                   (map (fn partize- [[_ s]] (keyword "db.part" (name (or (:part s) "user"))))
                        (chunk-schemas sdefs))))))

(defn schematize
  "Transform a seq of :namespace,schema pairs into transactable
  datomic schema."
  [sdefs tempid-fn]
  (let [schema (map (partial dsa/generate-schema tempid-fn)
                    (expand-schemas sdefs))
        partitions (partize sdefs tempid-fn)]
    (if (empty? partitions)
      schema
      (conj schema partitions))))

(defn- schemaload* [conn sdefs tempid-fn]
  (doall
   (map (partial d/transact conn)
        (schematize sdefs tempid-fn))))

(defn- load-fn* [conn fnspec tempid-fn]
  (d/transact conn [(merge {:db/id (tempid-fn :db.part/user)} fnspec)]))

(defn- load-fns* [conn fnspecs tempid-fn]
  (dorun
   (map (fn load-fns*- [fnspec]
          (load-fn* conn fnspec tempid-fn))
        fnspecs)))

(defn schemaload
  "Transact the specified schema definitions on the specified DB connection."
  ([conn sdefs]
     (schemaload conn sdefs d/tempid :constraints :tx-fns))
  ([conn sdefs tempid-fn & specs]
     (when (some #{:constraints} specs)
       (schemaload* conn constraints-schema tempid-fn))
     (when (some #{:tx-fns} specs)
       (load-fns* conn tx-fns tempid-fn))
     (schemaload* conn sdefs tempid-fn)))

(comment
  (def schema-full-sample
    {:db/id #db/id [:db.part/db]
     :db/ident :person/name
     :db/valueType [:db.type/keyword :db.type/string :db.type/boolean
                    :db.type/long :db.type/bigint :db.type/float
                    :db.type/double :db.type/bigdec :db.type/ref
                    :db.type/instant :db.type/uuid :db.type/uri
                    :db.type/bytes]
     :db/cardinality [:db.cardinality/one :db.cardinality/many]
     :db/doc "A person's name"
     :db/unique [:db.unique/value :db.unique/identity nil]
     :db/index [true false]
     :db/isComponent [true nil]
     :db/noHistory [true false]})

  (defonce db-url "datomic:mem://testdb")
  (d/create-database db-url)
  (map (partial d/transact (d/connect db-url))
       (schematize datomic-schematode.core-test/test-schemas d/tempid))

  )
