(ns invariant.core.count
  (:refer-clojure :exclude [count])
  (:require [invariant.core.invariant :refer [invariant]]))

(defn count
  "An invariant on the number of elements matching a given specter navigator and
   predicate."
  [{:keys [name sources predicate]} count-predicate & args]
  (let [reduce-fn (if predicate
                    (fn [counter value]
                      (if (predicate value)
                        (inc counter)
                        counter))
                    (fn [counter _]
                      (inc counter)))]
    (invariant
      {:name    name
       :sources sources
       :state   0
       :reduce  reduce-fn
       :verify  (fn [counter _] (apply count-predicate counter args))})))
