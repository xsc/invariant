(ns invariant.core.cycles-test
  (:require [clojure.test :refer :all]
            [invariant.core :as invariant]
            [invariant.core.protocols :as p]))

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
