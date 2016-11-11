(ns invariant.core.unique
  (:require [invariant.core.protocols :refer :all]))

(defn- find-duplicates
  [{:keys [data]} unique-by]
  (->> (group-by unique-by data)
       (keep
         (fn [[k vs]]
           (when (next vs)
             {:value      k
              :duplicates vs})))))

(defn- add-error
  [{:keys [path state] :as result} name {:keys [value duplicates]}]
  (let [error-data {:invariant/duplicate-value value}
        error (->invariant-error name path state duplicates error-data)]
    (update result :errors conj error)))

(deftype Unique [invariant name unique-by]
  Invariant
  (run-invariant [_ path state data]
    (let [result (run-invariant invariant path state data)
          duplicate-groups (find-duplicates result unique-by)]
      (reduce
        (fn [result duplicate-group]
          (add-error result name duplicate-group))
        result duplicate-groups))))
