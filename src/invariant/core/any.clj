(ns invariant.core.any
  (:require [invariant.core.protocols :refer :all]))

(deftype Any []
  Invariant
  (run-invariant [_ path state value]
    {:data   [value]
     :path   path
     :state  state
     :errors []})

  ;; to avoid surprises when expecting this to be a function.
  clojure.lang.IFn
  (invoke [this]
    this))

(extend-protocol Invariant
  nil
  (run-invariant [_ path state value]
    {:data   [value]
     :path   path
     :state  state
     :errors []}))
