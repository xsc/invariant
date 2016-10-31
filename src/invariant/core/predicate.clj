(ns invariant.core.predicate
  (:require [invariant.core.protocols :refer :all]))

(deftype Predicate [name predicate]
  Invariant
  (run-invariant [_ state value]
    (if (predicate state value)
      {:data   [value]
       :state  state
       :errors []}
      {:data   [value]
       :errors [(->InvariantError name state value)]
       :state  state})))
