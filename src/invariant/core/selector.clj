(ns invariant.core.selector
  (:require [invariant.core.protocols :refer :all]
            [com.rpl.specter :as specter]))

(deftype Selector [invariant path path-form]
  Invariant
  (run-invariant [_ path' state value]
    (let [result (if invariant
                   (run-invariant invariant path' state value)
                   (invariant-holds path' state value))]
      (-> result
          (update :path into path-form)
          (update :data #(into [] (specter/traverse [specter/ALL path] %)))))))
