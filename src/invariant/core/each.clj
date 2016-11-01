(ns invariant.core.each
  (:require [invariant.core.protocols :refer :all]))

(defn- run-each
  [{:keys [path state] :as result} each-invariant index element]
  (let [path' (conj path :invariant/each index)
        r (run-invariant each-invariant path' state element)]
    (update result :errors into (:errors r))))

(deftype Each [invariant each-invariant]
  Invariant
  (run-invariant [_ path state data]
    (let [result (run-invariant invariant path state data)]
      (->> (:data result)
           (map-indexed vector)
           (reduce
             (fn [result [index element]]
               (run-each result each-invariant index element))
             result)))))
