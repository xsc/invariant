(ns invariant.core.unique
  (:require [invariant.core.invariant :as i]))

(defn unique
  "A uniqueness invariant on all elements matching the specter navigator given
   in `:sources`. `:unique-by` can be used to generate the value that has to be
   unique for each element (default: `identity`)."
  [{:keys [name sources unique-by]
    :or {unique-by identity}}]
  (i/invariant
    {:name    name
     :sources sources
     :targets sources
     :state   {}
     :reduce  #(update %1 (unique-by %2) (fnil inc 0))
     :verify  (fn [fq v]
                (= (fq (unique-by v)) 1))}))
