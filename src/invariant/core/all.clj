(ns invariant.core.all
  (:require [invariant.core.protocols :refer :all]))

(deftype All [invariant all-invariant]
  Invariant
  (run-invariant [_ state data]
    (let [result               (run-invariant invariant state data)
          {:keys [state data]} result
          all-result           (run-invariant all-invariant state data)]
      (update result :errors into (:errors all-result)))))
