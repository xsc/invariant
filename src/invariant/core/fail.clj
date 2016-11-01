(ns invariant.core.fail
  (:require [invariant.core.protocols :refer :all]))

(deftype Fail [name]
  Invariant
  (run-invariant [_ path state value]
    {:data   [value]
     :state  state
     :errors [(error name path state value)]}))
