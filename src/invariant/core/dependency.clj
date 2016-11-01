(ns invariant.core.dependency
  (:require [invariant.core.protocols :refer :all]
            [com.rpl.specter :as specter]))

(deftype Dependency [invariant k path reduce-fn]
  Invariant
  (run-invariant [_ path' state value]
    (reduce
      #(update-in %1 [:state k] reduce-fn %2)
      (run-invariant invariant path' state value)
      (specter/select path value))))
