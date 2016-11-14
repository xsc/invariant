(ns invariant.core.and-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]
            [com.rpl.specter :refer :all]))

(deftest t-and-invariant
  (let [invariant (invariant/and
                    (-> (invariant/on [:a ALL])
                        (invariant/each
                          (invariant/value :is-a? #{:a})))
                    (-> (invariant/on [:b ALL])
                        (invariant/each
                          (invariant/value :is-b? #{:b}))))]
    (testing "invariant implementation."
      (is (satisfies? p/Invariant invariant)))
    (testing "valid document."
      (is (nil? (invariant/check
                  invariant
                  {:a [:a :a :a]
                   :b [:b :b]}))))
    (testing "invalid document."
      (let [errors (invariant/check
                     invariant
                     {:a [:a :x :a]
                      :b [:b :y]})]
        (is (= 2 (count errors)))
        (is (= #{[:is-a? [:x]] [:is-b? [:y]]}
               (set
                 (map
                   (juxt :invariant/name :invariant/values)
                   errors))))))))
