(ns datomic-schematode.core-test
  (:require [clojure.test :refer :all]
            [datomic-schematode.core :refer :all]))

(def test-schemas
  ;; [nod] to
  ;; https://github.com/Yuppiechef/datomic-schema/blob/master/README.md
  [:user {:attrs [[:username :string :indexed]
                  [:pwd :string "Hashed password string"]
                  [:email :string :indexed]
                  [:status :enum [:pending :active :inactive :cancelled]]
                  [:group :ref :many]]
          :part :app}
   :group {:attrs [[:name :string]
                   [:permission :string :many]]
           ;; testing without :part
           }])

(deftest expand-fields-test
  (testing "expand-fields"
    (is (= (expand-fields
            (get-in (apply hash-map datomic-schematode.core-test/test-schemas)
                    [:user :attrs]))
           {"group" [:ref #{:many}],
            "status" [:enum #{[:pending :active :inactive :cancelled]}],
            "email" [:string #{:indexed}],
            "pwd" [:string #{"Hashed password string"}],
            "username" [:string #{:indexed}]}))))

(deftest expand-schemas-test
  (testing "expand-schemas"
    (is (= (expand-schemas test-schemas)
           [{:part :db.part/user, :namespace "schematode-constraint", :name "schematode-constraint", :basetype :schematode-constraint, :fields {"desc" [:string #{}], "name" [:string #{:indexed}]}} {:part :db.part/app, :namespace "user", :name "user", :basetype :user, :fields {"group" [:ref #{:many}], "status" [:enum #{[:pending :active :inactive :cancelled]}], "email" [:string #{:indexed}], "pwd" [:string #{"Hashed password string"}], "username" [:string #{:indexed}]}} {:part :db.part/user, :namespace "group", :name "group", :basetype :group, :fields {"permission" [:string #{:many}], "name" [:string #{}]}}]))))

(deftest schematize-test
  (testing "schematize"
    (is (= (schematize test-schemas (constantly -1))
           '([{:db/id -1, :db/ident :db.part/app, :db.install/_partition :db.part/db}] ({:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :schematode-constraint/desc, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index true, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :schematode-constraint/name, :db/id -1}) ({:db/noHistory false, :db/cardinality :db.cardinality/many, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/ref, :db/ident :user/group, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/ref, :db/ident :user/status, :db/id -1} [:db/add -1 :db/ident :user.status/pending] [:db/add -1 :db/ident :user.status/active] [:db/add -1 :db/ident :user.status/inactive] [:db/add -1 :db/ident :user.status/cancelled] {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index true, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :user/email, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "Hashed password string", :db/isComponent false, :db/valueType :db.type/string, :db/ident :user/pwd, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index true, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :user/username, :db/id -1}) ({:db/noHistory false, :db/cardinality :db.cardinality/many, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :group/permission, :db/id -1} {:db/noHistory false, :db/cardinality :db.cardinality/one, :db.install/_attribute :db.part/db, :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false, :db/valueType :db.type/string, :db/ident :group/name, :db/id -1}))))))
