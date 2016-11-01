(ns invariant.core.predicate
  (:require [invariant.core.protocols :refer :all]))

(deftype Predicate [name predicate]
  Invariant
  (run-invariant [_ path state value]
    (if (predicate state value)
      {:data   [value]
       :path   path
       :state  state
       :errors []}
      {:data   [value]
       :path   []
       :errors [(error name path state value)]
       :state  state})))
