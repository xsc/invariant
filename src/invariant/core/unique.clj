(ns invariant.core.unique
  (:require [invariant.core.protocols :refer :all]))

(defn- generate-frequencies
  [{:keys [data]} unique-by]
  (frequencies (map unique-by data)))

(defn- unique?
  [fq value]
  (= (get fq value) 1))

(defn- add-error
  [{:keys [path state] :as result} name index element]
  (->> (error name (conj path index) state element)
       (update result :errors conj)))

(deftype Unique [invariant name unique-by]
  Invariant
  (run-invariant [_ path state data]
    (let [result (run-invariant invariant path state data)
          fq (generate-frequencies result unique-by)]
      (->> (:data result)
           (map-indexed vector)
           (reduce
             (fn [result [index element]]
               (let [value (unique-by element)]
                 (if-not (unique? fq value)
                   (add-error result name index element)
                   result)))
             result)))))
