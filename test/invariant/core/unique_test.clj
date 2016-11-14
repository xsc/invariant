(ns invariant.core.unique-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]
            [com.rpl.specter :refer :all]))

(deftest t-uniqueness-invariant
  (let [invariant (-> (invariant/on [:elements ALL])
                      (invariant/unique :unique? {:unique-by :name}))]
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
                     {:elements [{:name "a", :index 0}
                                 {:name "a", :index 1}
                                 {:name "c"}]})
            {:keys [invariant/name
                    invariant/error-context
                    invariant/values]} (first errors)]
        (is (= 1 (count errors)))
        (is (= :unique? name))
        (is (= (set [{:name "a", :index 0}
                     {:name "a", :index 1}])
               (set values)))
        (is (= error-context {:invariant/duplicate-value "a"}))))))
