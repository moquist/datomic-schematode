(ns datomic-schematode.core
  (:require [datomic.api :as d]
            [incanter.stats :as stats]
            [datomic-schema.schema :as dsa]
            [datomic-schematode.constraints.support :as dsc-support]))

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
          sdefs))

(defn partize
  "Transform a seq of :namespace,schema pairs into a seq of
  transactable partition-installation maps."
  [sdefs tempid-fn]
  (reduce (partial dsa/part-to-datomic tempid-fn)
          []
          (remove #{:db.part/user}
                  (distinct
                   (map (fn partize- [[_ s]] (keyword "db.part" (name (or (:part s) "user"))))
                        sdefs)))))

(defn namespace-dbfns [sname dbfns]
  (map (fn namespace-dbfns- [dbfn]
         (let [ident (keyword (name sname) (name (:db/ident dbfn)))]
           (merge dbfn {:db/ident ident})))
       dbfns))

(defn extract-dbfns
  "Extract db/fn specs from sdefs."
  [sdefs]
  (flatten
   (remove nil?
           (map (fn extract-dbfns- [[sname sdef]] (namespace-dbfns sname (:dbfns sdef)))
                sdefs))))

(defn dbfnize
  "Process sdefs and return a seq of transactable db/fn entity
   definitions."
  [sdefs tempid-fn]
  (let [fns (extract-dbfns sdefs)]
    (map (fn dbfnize- [f]
           (merge {:db/id (tempid-fn :db.part/user)} f))
         fns)))

(defn schematize
  "Transform a seq of :namespace,schema pairs into transactable
  datomic schema."
  [sdefs tempid-fn]
  (let [schema (map (partial dsa/generate-schema tempid-fn)
                    (expand-schemas sdefs))
        partitions (partize sdefs tempid-fn)
        ;; TODO: handle fns here
        fns (dbfnize () sdefs)]
    (if (empty? partitions)
      schema
      (conj schema partitions))))

(defn- load-schema!*
  "Transact sdefs into conn, using tempid-fn.
   Return seq of tx promises."
  [conn sdefs tempid-fn]
  (doall (map (partial d/transact conn)
              (conj (schematize sdefs tempid-fn)
                    (dbfnize sdefs tempid-fn)))))

(defn init-schematode-constraints!
  "Initialize schematode constraint schema and tx-fns."
  ([conn]
     (init-schematode-constraints! conn d/tempid))
  ([conn tempid-fn]
     (load-schema!* conn dsc-support/constraints-schema tempid-fn)))

;; TODO: handle any resource that can be opened by io/reader.
(defn load-schema!
  "Transact the specified schema definitions on the specified DB connection."
  ([conn sdefs]
     (load-schema! conn sdefs d/tempid))
  ([conn sdefs tempid-fn]
     (load-schema!* conn sdefs tempid-fn)))

(defn constraint-cost-stats [conn]
  (let [db (d/db conn)
        query '[:find ?e :where [?e :schematode-constraint/elapsed-msec]]
        costs (map #(:schematode-constraint/elapsed-msec (d/entity db (first %)))
                   (d/q query db))
        cnt (count costs)
        total (reduce + costs)
        mean (/ total cnt)
        median (stats/median costs)]
    {:mean-msec mean
     :median-msec median
     :tx-count cnt
     :total-msec total}))

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
  )
