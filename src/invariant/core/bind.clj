(ns invariant.core.bind
  (:require [invariant.core.protocols :refer :all]))

(deftype Bind [bind-fn]
  Invariant
  (run-invariant [_ state data]
    (let [invariant (bind-fn state data)]
      (run-invariant invariant state data))))
