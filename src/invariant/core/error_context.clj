(ns invariant.core.error-context
  (:require [invariant.core.protocols :refer :all]))

(deftype ErrorContext [invariant context-fn]
  Invariant
  (run-invariant [_ path state value]
    (let [result  (run-invariant invariant path state value)]
      (if (seq (:errors result))
        (->> (context-fn state value)
             (merge-error-context result))
        result))))
