(ns user
  (:require [com.rpl.specter :refer :all]
            [invariant.core :as invariant]))

(def invariant
  (-> (invariant/on [:body (walker :variable) (collect-one) :variable (must :name)])
      (invariant/fmap first)
      (invariant/with
        :declared-variables [:declarations ALL :name] conj #{})
      (invariant/each
        (invariant/predicate
          :validator/variables-declared
          (fn [{:keys [declared-variables]} v]
            (contains? declared-variables (-> v :variable :name)))))))

(defn verify-arg-count
  [args expected]
  (let [arg-count (count args)]
    (when (not= arg-count expected)
      (invariant/fail [:invalid-arg-count
                       {:expected expected, :given arg-count}]))))

(def function-invariant
  (invariant/recursive
    [self]
    (invariant/and
      (invariant/bind
        (fn [_ {:keys [function args]}]
          (case function
            "F" (verify-arg-count args 3)
            "G" (verify-arg-count args 0)
            nil)))
      (-> (invariant/on [:args (walker :function)])
          (invariant/each self)))))

(time
  (invariant/run
    (-> (invariant/on [(walker :function)])
        (invariant/each function-invariant))
    function-invariant
    {:declarations [{:name "a"} {:name "b"}]
     :body [{:function "F"
             :args [{:variable {:name "a"}}
                    #_{:variable {:name "b"}}
                    {:function "G"
                     :args []}]}]}))

(invariant/run
  (invariant/recursive
    [self]
    (invariant/and
      (invariant/predicate :value-int? #(integer? (:value %2)))
      (-> (invariant/on [:children ALL])
          (invariant/each self))))
  {:value 1
   :children
   [{:value :x
     :children [{:value 4}]}]})
