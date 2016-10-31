(ns invariant.core.dependency
  (:require [invariant.core.protocols :refer :all]
            [com.rpl.specter :as specter]))

(deftype Dependency [invariant k path reduce-fn initial-state]
  Invariant
  (run-invariant [_ state value]
    (let [inner-state (->> (specter/traverse path value)
                           (reduce reduce-fn initial-state))]
      (-> (run-invariant invariant state value)
          (assoc-in [:state k] inner-state)))))
