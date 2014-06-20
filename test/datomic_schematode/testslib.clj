(ns datomic-schematode.testslib)

;; TODO: see doc for clojure.test/is (DOH!)
(defmacro should-throw
  "Borrowed from https://github.com/Datomic/day-of-datomic .
   Runs forms, expecting an exception. Returns exception message if an
   exception occurred, and false if no exception occurred."
  [& forms]
  `(try
     ~@forms
     false
     (catch Exception t#
       (str "Got expected exception:\n\t" (.getMessage t#)))))

(defn ensure-tx [tx]
  (= '(:db-before :db-after :tx-data :tempids) (keys @tx)))

(defn ensure-seq-txs [txs]
  (every? ensure-tx txs))

(defn predict-entity-map [e]
  (merge (into {} e) {:db/id -1}))
