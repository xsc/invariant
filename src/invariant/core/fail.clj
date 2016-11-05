(ns invariant.core.fail
  (:require [invariant.core.protocols :refer :all]))

(deftype Fail [name]
  Invariant
  (run-invariant [_ path state value]
    (invariant-failed name path state value)))
