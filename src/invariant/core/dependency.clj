(ns invariant.core.dependency
  (:require [invariant.core.protocols :refer :all]
            [com.rpl.specter :as specter]))

(deftype Dependency [invariant k path reduce-fn initial-value]
  Invariant
  (run-invariant [_ path' state value]
    (let [result (run-invariant invariant path' state value)]
      (->> (specter/traverse path value)
           (reduce reduce-fn initial-value)
           (assoc-in result [:state k])))))
