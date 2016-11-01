(ns invariant.core.fmap
  (:require [invariant.core.protocols :refer :all]))

(deftype FMap [invariant f]
  Invariant
  (run-invariant [_ path state data]
    (-> (run-invariant invariant path state data)
        (update :data #(mapv f %)))))
