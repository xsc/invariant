(ns invariant.core.any
  (:require [invariant.core.protocols :refer :all]))

(deftype Any []
  Invariant
  (run-invariant [_ state value]
    {:data   [value]
     :state  state
     :errors []}))

(extend-protocol Invariant
  nil
  (run-invariant [_ state value]
    {:data   [value]
     :state  state
     :errors []}))
