(ns invariant.core-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]
            [com.rpl.specter :refer :all]))

(deftest t-simple-invariant
  (let [invariant
        (-> (invariant/collect-as :declared-variables [:declarations ALL :name])
            (invariant/on [:body (walker :variable) :variable (must :name)])
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

(deftest t-acyclic-invariant
  (let [invariant (-> (invariant/on [:elements])
                      (invariant/is?
                        (invariant/acyclic
                          :acyclic?
                          #(into {} (map (juxt :name (comp set :children))) %2)
                          #(into {} (map (juxt :name identity)) %2))))]
    (testing "invariant implementation."
      (is (satisfies? p/Invariant invariant)))
    (testing "valid document."
      (is (nil? (invariant/check
                  invariant
                  {:elements [{:name "a", :children ["b"]}
                              {:name "b", :children ["c"]}
                              {:name "c"}]}))))
    (testing "invalid document."
      (let [errors (invariant/check
                     invariant
                     {:elements [{:name "a", :children ["b"]}
                                 {:name "b", :children ["c"]}
                                 {:name "c", :children ["b" "c"]}]})
            {:keys [invariant/name
                    invariant/error-context
                    invariant/values]} (first errors)]
        (is (= 1 (count errors)))
        (is (= :acyclic? name))
        (is (= (set [{:name "b", :children ["c"]}
                     {:name "c", :children ["b" "c"]}])
               (set values)))
        (is (= error-context
               {:invariant/cycle #{"b" "c"}
                :invariant/edges {"b" #{"c"}, "c" #{"b" "c"}}}))))))
