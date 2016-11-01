(ns invariant.core.and
  (:require [invariant.core.protocols :refer :all]))

(deftype And [invariants]
  Invariant
  (run-invariant [_ path state data]
    (->> invariants
         (map #(run-invariant % path state data))
         (reduce
           (fn [result more]
             (update result :errors into (:errors more)))))))
