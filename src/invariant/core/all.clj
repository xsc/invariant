(ns invariant.core.all
  (:require [invariant.core.protocols :refer :all]))

(deftype All [invariant all-invariant]
  Invariant
  (run-invariant [_ path state value]
    (let [result  (run-invariant invariant path state value)
          {:keys [state path value]} result]
      (->> (run-invariant all-invariant path state value)
           (merge-results result)))))
