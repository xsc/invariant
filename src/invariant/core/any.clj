(ns invariant.core.any
  (:require [invariant.core.protocols :refer :all]))

(deftype Any []
  Invariant
  (run-invariant [_ path state value]
    (invariant-holds path state value)))

(extend-protocol Invariant
  nil
  (run-invariant [_ path state value]
    (invariant-holds path state value)))
