(ns invariant.core.invariant
  (:require [invariant.core.protocols :as p]
            [invariant.core.engine :as engine]
            [invariant.error :as error]
            [com.rpl.specter :as specter]))

(defrecord Invariant [name sources targets state reduce verify]
  p/Invariant
  (invariant-name [_]
    name)
  (sources [_]
    sources)
  (targets [_]
    targets)
  (invariant-state [_]
    state)
  (invariant-reduce [_ state source]
    (reduce state source))
  (invariant-verify [_ state target]
    (verify state target))
  (report-broken-invariant [invariant state value]
    (error/broken-invariant
      (p/invariant-name invariant)
      state
      value))

  clojure.lang.IFn
  (invoke [this data]
    (engine/run this data)))

(alter-meta! #'->Invariant assoc :private true)
(alter-meta! #'map->Invariant assoc :private true)

(defn invariant
  "Generate an invariant describing the relationship between two subsets of
   a given piece of data.

   - `:name`: the name of the invariant (needs to be a namespaced keyword),
   - `:sources`: specter paths to elements the invariant state is built from,
   - `:targets`: specter paths to elements that are verified using the invariant,
   - `:state`: the initial invariant state,
   - `:reduce`: a reducer function to be applied to `:state` and source items,
   - `:verify`: a predicate applied to verification state and target items.

   Internally, the incoming data is traversed using `:sources` paths and each
   element passed to `:reduce`. Afterwards, the final state is used to verify
   all `:targets` paths using `:verify`.

   For example, to create an invariant between variable declarations and usages:

   ```clojure
   (invariant/invariant
     {:name    :validator/variables-have-been-declared-before-usage
      :sources [:declarations ALL :name]
      :targets [:body (walker :variable) :variable (must :name)]
      :state   #{}
      :reduce  conj
      :verify  contains?})
   ```

   This will collect all variable names and store them in a set, before
   verifying that the name of each used variable appears within said set."
  [{:keys [name sources targets state reduce verify]
    :or {targets specter/STAY
         state   ()
         reduce  conj}}]
  {:pre [(keyword? name) (namespace name)
         sources
         targets
         (fn? reduce)
         (fn? verify)]}
  (->Invariant
    name
    (specter/comp-paths sources)
    (specter/comp-paths targets)
    state
    reduce
    verify))
