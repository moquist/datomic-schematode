(ns datomic-schematode.constraints.support
  (:require [datomic.api :as d]))

(def constraints-schema
  [:schematode-constraint {:attrs [[:name :string :indexed]
                                   [:desc :string]]
                           :part :user}])

(def tx-fns
  ;; TODO: add timing instrumentation and add attributes to the TX so we know the cost of schematode constraints
  ;;     * look at timbre, maybe
  ;;     * criterium seems more appropriate for this: https://github.com/hugoduncan/criterium
  ;; TODO: support warnings without exceptions
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

