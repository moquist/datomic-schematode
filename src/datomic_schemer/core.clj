(ns datomic-schemer.core
  (:require [datomic.api :as d]
            [datomic-schema.schema :as ds]))

(defn expand-fields
  "fn replacement for datomic-schema.schema/fields.
  Takes in any number of field vectors like [fname type & opts].
  Returns a map suitable for processing by
  datomic-schema.schema/field-to-datomic."
  [fdefs]
  (reduce (fn -fields [a [fname type & opts]]
            (assoc a
              (name fname)
              [type (set opts)]))
          {}
          fdefs))

(defn expand-schemas
  "Transform a seq of :namespace,schema pairs into a seq of
  schema-expression maps suitable for processing by
  datomic-schema.schema/generate-schema."
  [sdefs]
  (reduce (fn -scheme [a [sname sdef]]
            (let [part (keyword "db.part" (name (:part sdef)))
                  sname (name sname)
                  attrs (expand-fields (:attrs sdef))]
              (conj a
                    {:part part
                     :namespace sname
                     :name sname
                     :basetype (keyword sname)
                     :fields attrs})))
          []
          (partition 2 sdefs)))

(defn schematize
  "Transform a seq of :namespace,schema pairs into transactable
  datomic schema."
  [sdefs tempid-fn]
  (map (partial ds/generate-schema tempid-fn)
       (expand-schemas sdefs)))

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
       (schematize datomic-schemer.core-test/test-schemas d/tempid))

  )
