(ns invariant.core.any
  (:require [invariant.core.protocols :refer :all]))

(deftype Any []
  Invariant
  (run-invariant [_ path state value]
    {:data   [value]
     :path   path
     :state  state
     :errors []}))

(extend-protocol Invariant
  nil
  (run-invariant [_ path state value]
    {:data   [value]
     :path   path
     :state  state
     :errors []}))
