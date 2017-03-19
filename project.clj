(defproject invariant "0.1.5-SNAPSHOT"
  :description "Invariants on Clojure Data Structures"
  :url "https://github.com/xsc/invariant"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2016
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [com.rpl/specter "1.0.0"]]
  :profiles
  {:dev   {:dependencies [[org.clojure/test.check "0.9.0"]]}
   :codox {:dependencies [[codox-theme-rdash "0.1.1"]]
           :plugins [[lein-codox "0.10.0"]]
           :codox {:project {:name "invariant"}
                   :metadata {:doc/format :markdown}
                   :themes [:rdash]
                   :source-paths ["src"]
                   :source-uri "https://github.com/xsc/invariant/blob/master/{filepath}#L{line}"
                   :namespaces [invariant.core
                                invariant.core.protocols
                                invariant.debug
                                invariant.spec]}}}
  :aliases {"codox" ["with-profile" "+codox" "codox"]}
  :pedantic? :abort)
