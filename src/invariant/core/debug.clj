(ns invariant.core.debug
  (:require [invariant.core.protocols :refer :all]))

(deftype Debug [k invariant debug-fn]
  Invariant
  (run-invariant [_ path state value]
    (let [result (run-invariant invariant path state value)]
      (debug-fn k path state value result)
      result)))
