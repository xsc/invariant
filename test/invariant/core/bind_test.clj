(ns invariant.core.bind-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]
            [com.rpl.specter :refer :all]))

(deftest t-bind-invariant
  (let [invariant (-> (invariant/on [ALL])
                      (invariant/each
                        (invariant/bind
                          (fn [_ [k _]]
                            (-> (invariant/on [LAST ALL])
                                (invariant/each
                                  (invariant/value :matches-key? #{k})))))))]
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
        (is (= #{[:matches-key? [:x]] [:matches-key? [:y]]}
               (set
                 (map
                   (juxt :invariant/name :invariant/values)
                   errors))))))))
