(ns invariant.core.fail
  (:require [invariant.core.protocols :refer :all]))

(deftype Fail [name]
  Invariant
  (run-invariant [_ state value]
    {:data   [value]
     :state  state
     :errors [(->InvariantError name state value)]}))
