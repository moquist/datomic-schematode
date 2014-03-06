(ns datomic-schematode.core
  (:require [datomic-schema.schema :as dsa]))

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
