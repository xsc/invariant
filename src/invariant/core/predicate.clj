(ns invariant.core.predicate
  (:require [invariant.core.protocols :refer :all]))

(deftype Predicate [name predicate]
  Invariant
  (run-invariant [_ path state value]
    (if (predicate state value)
      (invariant-holds path state value)
      (invariant-failed name path state value))))
