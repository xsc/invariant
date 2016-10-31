(ns invariant.core.each
  (:require [invariant.core.protocols :refer :all]))

(deftype Each [invariant each-invariant]
  Invariant
  (run-invariant [_ state data]
    (let [result (run-invariant invariant state data)]
      (reduce
        (fn [{:keys [state] :as result} element]
          (let [{:keys [errors]} (run-invariant each-invariant state element)]
            (update result :errors into errors)))
        result (:data result)))))
