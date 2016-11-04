(ns invariant.core.is
  (:require [invariant.core.protocols :refer :all]))

(deftype Is [invariant self-invariant]
  Invariant
  (run-invariant [_ path state value]
    (let [result (run-invariant invariant path state value)
          {:keys [state path data]} result]
      (when (next data)
        (throw
          (IllegalStateException.
            (str
              "'is?' can only be used with a selector that returns at most one "
              "element.\n"
              "current data: " (pr-str data)))))
      (if (seq data)
        (->> (run-invariant self-invariant path state (first data))
             (merge-results result))
        result))))
