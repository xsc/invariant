(ns invariant.core-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]
            [com.rpl.specter :refer :all]))

(deftest t-simple-invariant
  (let [invariant
        (-> (invariant/on [:body (walker :variable) :variable (must :name)])
            (invariant/collect-as :declared-variables [:declarations ALL :name])
            (invariant/each
              (invariant/property
                :validator/variables-declared
                (fn [{:keys [declared-variables]} variable-name]
                  (contains? declared-variables variable-name)))))]
    (testing "invariant implementation."
      (is (satisfies? p/Invariant invariant)))
    (testing "valid document."
      (is (nil? (invariant/check
                  invariant
                  {:declarations [{:name "a"} {:name "b"}]
                   :body [{:type :function
                           :name "F"
                           :args [{:variable {:name "a"}}
                                  {:variable {:name "b"}}]}]}))))
    (testing "invalid document."
      (let [errors (invariant/check
                     invariant
                     {:declarations [{:name "a"} {:name "b"}]
                      :body [{:type :function
                              :name "F"
                              :args [{:variable {:name "x"}}
                                     {:variable {:name "y"}}]}]})]
        (is (seq errors))
        (is (= #{["x"] ["y"]}
               (into #{} (map :invariant/values errors))))
        (is (every? (comp #{#{"a" "b"}} :declared-variables :invariant/state) errors))))))
