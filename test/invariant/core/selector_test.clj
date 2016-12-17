(ns invariant.core.selector-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]
            [com.rpl.specter :refer :all]))

(deftest t-selector-invariant
  (let [invariant (invariant/on [:elements ALL])]
    (testing "invariant implementation."
      (is (satisfies? p/Invariant invariant)))
    (testing "selection."
      (let [{:keys [data errors path state]}
            (invariant/run
              invariant
              {:elements [{:name "a"}
                          {:name "b"}
                          {:name "c"}]})]
        (is (empty? errors))
        (is (= [{:name "a"} {:name "b"} {:name "c"}] data))
        (is (= [:elements] path))
        (is (= {} state))))))

(deftest t-nested-selector-invariant
  (let [invariant (-> (invariant/on [:elements ALL])
                      (invariant/on [:name]))]
    (testing "invariant implementation."
      (is (satisfies? p/Invariant invariant)))
    (testing "selection."
      (let [{:keys [data errors path state]}
            (invariant/run
              invariant
              {:elements [{:name "a"}
                          {:name "b"}
                          {:name "c"}]})]
        (is (empty? errors))
        (is (= ["a" "b" "c"] data))
        (is (= [:elements :name] path))
        (is (= {} state))))))

(deftest t-computed-selector
  (let [all-elements #(mapcat :elements %2)
        invariant (invariant/on-values all-elements)]
    (testing "invariant implementation."
      (is (satisfies? p/Invariant invariant)))
    (testing "selection."
      (let [{:keys [data errors path state]}
            (invariant/run
              invariant
              {:elements [{:name "a"}
                          {:name "b"}
                          {:name "c"}]})]
        (is (empty? errors))
        (is (= [{:name "a"} {:name "b"} {:name "c"}] data))
        (is (= ['all-elements] path))
        (is (= {} state))))))
