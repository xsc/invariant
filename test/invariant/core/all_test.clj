(ns invariant.core.all-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]
            [com.rpl.specter :refer :all]))

(deftest t-all-invariant
  (let [invariant (-> (invariant/on [:elements ALL :name])
                      (invariant/all
                        (invariant/value :all-strings? #(every? string? %))))]
    (testing "invariant implementation."
      (is (satisfies? p/Invariant invariant)))
    (testing "valid document."
      (is (nil? (invariant/check
                  invariant
                  {:elements [{:name "a"}
                              {:name "b"}
                              {:name "c"}]}))))
    (testing "invalid document."
      (let [errors (invariant/check
                     invariant
                     {:elements [{:name "c"}
                                 {:name :b}
                                 {:name "a"}]})
            {:keys [invariant/name
                    invariant/error-context
                    invariant/values]} (first errors)]
        (is (= 1 (count errors)))
        (is (= :all-strings? name))
        (is (= (set [["c" :b "a"]]) (set values)))))))
