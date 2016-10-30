(ns invariant.core.and
  (:refer-clojure :exclude [and])
  (:require [invariant.core.protocols :as p]
            [invariant.core.engine :as engine]
            [com.rpl.specter :as specter]))

;; ## Helpers

(defn- combine-navigators
  [navigators]
  (->> (map-indexed
         (fn [index navigator]
           [(specter/putval index) navigator])
         navigators)
       (apply specter/multi-path)
       (specter/comp-paths)))

(defn- generate-invariant-name
  [invariants]
  (into [:invariant/and] (map p/invariant-name invariants)))

(defn- states-by-index
  [invariants]
  (into {} (map-indexed
             #(vector %1 (p/invariant-state %2))
             invariants)))

(defn- invariants-by-index
  [invariants]
  (into {} (map-indexed vector invariants)))

(defn- reduce-via-index
  [index->invariant index->state [index value]]
  (let [i (index->invariant index)]
    (update index->state
            index
            #(p/invariant-reduce i % value))))

(defn- verify-via-index
  [index->invariant index->state [index value]]
  (p/invariant-verify
    (index->invariant index)
    (index->state index)
    value))

(defn- report-via-index
  [index->invariant index->state [index value]]
  (p/report-broken-invariant
    (index->invariant index)
    (index->state index)
    value))

;; ## Invariant

(defrecord AndInvariant [name sources targets index->state index->invariant]
  p/Invariant
  (invariant-name [_]
    name)
  (sources [_]
    sources)
  (targets [_]
    targets)
  (invariant-state [_]
    index->state)
  (invariant-reduce [_ state value]
    (reduce-via-index index->invariant state value))
  (invariant-verify [_ state value]
    (verify-via-index index->invariant state value))
  (report-broken-invariant [_ state value]
    (report-via-index index->invariant state value))

  clojure.lang.IFn
  (invoke [this data]
    (engine/run this data)))

(alter-meta! #'->AndInvariant assoc :private true)
(alter-meta! #'map->AndInvariant assoc :private true)

(defn and*
  "Identical to [[and]], taking a seq of invariants."
  [invariants]
  {:pre [(next invariants)]}
  (let [sources          (combine-navigators (map p/sources invariants))
        targets          (combine-navigators (map p/targets invariants))
        and-name         (generate-invariant-name invariants)
        index->state     (states-by-index invariants)
        index->invariant (invariants-by-index invariants)]
    (->AndInvariant and-name sources targets index->state index->invariant)))

(defn and
  "Ensure that all the given invariants hold.

   ```clojure
   (invariant/and
     (invariant/unique
       {:name :validator/variable-declarations-unique
        ...})
     (invariant/count
       {:name :validator/at-least-one-function-call
        ...}))
   ```

   Invariant sources, targets and state will be merged internally and a combined
   seq of all broken invariants will be returned."
  [& invariants]
  (and* invariants))
