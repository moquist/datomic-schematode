(defproject datomic-schematode "0.1.1"
  :description "lets you express concise Datomic schema with DB constraints"
  :url "https://github.com/vlacs/datomic-schematode"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [datomic-schema "1.0.2"]
                 [incanter/incanter-core "1.5.4"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [com.datomic/datomic-free "0.9.4572"]]}}
  :plugins [[lein-cloverage "1.0.2"]])
