(ns invariant.core.dependency-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]
            [com.rpl.specter :refer :all]))

(deftest t-invariant-reduce-dependencies
  (let [invariant (-> (invariant/on [ALL])
                      (invariant/each
                        (-> (invariant/collect-as :expected-val? [FIRST])
                            (invariant/on [LAST ALL])
                            (invariant/each
                              (invariant/property
                                :matches-key?
                                (fn [{:keys [expected-val?]} v]
                                (expected-val? v)))))))]
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

(deftest t-invariant-computed-dependency
  (let [invariant (-> (invariant/on [ALL])
                      (invariant/each
                        (-> (invariant/collect-as :collected-key [FIRST])
                            (invariant/compute-as
                              :expected-val
                              (fn [{:keys [collected-key]} [[k _]]]
                                (keyword
                                  (str
                                    (name (first collected-key))
                                    (name k)))))
                            (invariant/on [LAST ALL])
                            (invariant/each
                              (invariant/property
                                :matches-key?
                                (fn [{:keys [expected-val]} v]
                                  (= expected-val v)))))))]
    (testing "invariant implementation."
      (is (satisfies? p/Invariant invariant)))
    (testing "valid document."
      (is (nil? (invariant/check
                  invariant
                  {:a [:aa :aa :aa]
                   :b [:bb :bb]}))))
    (testing "invalid document."
      (let [errors (invariant/check
                     invariant
                     {:a [:aa :x :aa]
                      :b [:bb :y]})]
        (is (= 2 (count errors)))
        (is (= #{[:matches-key? [:x]] [:matches-key? [:y]]}
               (set
                 (map
                   (juxt :invariant/name :invariant/values)
                   errors))))))))

(deftest t-first-element-dependency
  (let [invariant (-> (invariant/on [ALL])
                      (invariant/each
                        (-> (invariant/first-as :expected-val [FIRST])
                            (invariant/on [LAST ALL])
                            (invariant/each
                              (invariant/property
                                :matches-key?
                                (fn [{:keys [expected-val]} v]
                                  (= expected-val v)))))))]
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
