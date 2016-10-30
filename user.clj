(ns user)

(require '[invariant.core :as i]
         '[invariant.specter :refer [dfs]]
         '[com.rpl.specter :refer :all])

(def verify-declaration-before-usage
  (i/invariant
    {:name :validator/variables-have-been-declared-before-usage
     :sources [:declarations ALL :name]
     :targets [:body (dfs :variable) (must :name)]
     :state   #{}
     :reduce  conj
     :verify  contains?}))

(def x
  (i/and verify-declaration-before-usage
         (i/unique
           {:name :validator/declarations-unique
            :sources [:declarations ALL (collect-one) :name]
            :unique-by :name})))


#_(prn (verify-declaration-before-usage
       {:declarations [{:name "a", :type "int"}
                       {:name "b", :type "float"}]
        :body [{:function-name "+"
                :function-args [{:variable {:name "a"}}
                                {:function-name "*"
                                 :function-args [{:variable {:name "b"}}
                                                 {:variable {:name "b"}}]}]}]}))

#_(clojure.pprint/pprint
  (i/debug
    verify-declaration-before-usage
    {:declarations [{:name "a", :type "int"}
                    {:name "b", :type "float"}]
     :body [{:function-name "+"
             :function-args [{:variable {:name "a"}}
                             {:variable {:name "b"}}]}]}))

(clojure.pprint/pprint (i/debug x
      {:declarations [{:name "a", :type "int"}
                      {:name "a", :type "int"}]
       :body [{:function-name "+"
               :function-args [{:variable {:name "a"}}
                               {:variable {:name "b"}}]}]}))

(i/debug
  (i/count
    {:name :validator/at-most-five-queries
     :sources [:operations ALL #(= (:type %) :query)]}
    <= 5)
  {:operations (repeat 6 {:type :query})}
  )
