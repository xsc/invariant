(ns invariant.core.bind
  (:require [invariant.core.protocols :refer :all]))

(deftype Bind [bind-fn]
  Invariant
  (run-invariant [_ path state data]
    (let [invariant (bind-fn state data)]
      (run-invariant invariant path state data))))
