(ns invariant.core.dependency
  (:require [invariant.core.protocols :refer :all]
            [com.rpl.specter :as specter]))

(defn- reduce-for-state
  [path reduce-fn initial-value value]
  (->> (specter/traverse [specter/ALL path] value)
       (reduce reduce-fn initial-value)))

(deftype ReduceDependency [invariant k path reduce-fn initial-value]
  Invariant
  (run-invariant [_ path' state value]
    (let [{:keys [data] :as result}
          (if invariant
            (run-invariant invariant path' state value)
            (invariant-holds path' state value))
          state-value (reduce-for-state path reduce-fn initial-value data)]
      (assoc-in result [:state k] state-value))))

(deftype FirstDependency [invariant k path]
  Invariant
  (run-invariant [_ path' state value]
    (let [{:keys [data] :as result}
          (if invariant
            (run-invariant invariant path' state value)
            (invariant-holds path' state value))
          state-value (specter/select-one [specter/ALL path] data)]
      (assoc-in result [:state k] state-value))))

(deftype ComputedDependency [invariant k f path]
  Invariant
  (run-invariant [_ path' state value]
    (let [{:keys [data state] :as result}
          (if invariant
            (run-invariant invariant path' state value)
            (invariant-holds path' state value))
          relevant-data (if path
                          (specter/select data [specter/ALL path])
                          data)
          state-value (f state relevant-data)]
      (assoc-in result [:state k] state-value))))
