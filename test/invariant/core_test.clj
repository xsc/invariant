(ns invariant.core-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]
            [com.rpl.specter :refer :all]))

(deftest t-simple-invariant
  (let [invariant (invariant/invariant
                    {:name    :validator/variables-declared
                     :sources [:declarations ALL :name]
                     :targets [:body (walker :variable) :variable (must :name)]
                     :state   #{}
                     :reduce  conj
                     :verify  contains?})]
    (testing "invariant implementation."
      (is (instance? clojure.lang.IFn invariant))
      (is (satisfies? p/Invariant invariant)))
    (testing "valid document."
      (is (nil? (invariant/run
                  invariant
                  {:declarations [{:name "a"} {:name "b"}]
                   :body [{:type :function
                           :name "F"
                           :args [{:variable {:name "a"}}
                                  {:variable {:name "b"}}]}]}))))
    (testing "invalid document."
      (let [errors (invariant/run
                     invariant
                     {:declarations [{:name "a"} {:name "b"}]
                      :body [{:type :function
                              :name "F"
                              :args [{:variable {:name "x"}}
                                     {:variable {:name "y"}}]}]})]
        (is (seq errors))
        (is (= #{"x" "y"} (into #{} (map :value errors))))
        (is (every? (comp #{#{"a" "b"}} :state) errors))))))
