(ns invariant.core
  "Invariants on Clojure data structures."
  (:require [invariant.error :as error]
            [com.rpl.specter :as specter]))

;; ## Protocol

(defprotocol Invariant
  (invariant-name [invariant]
    "The name of the invariant, given as a namespaced keyword.")
  (sources [invariant]
    "A specter navigator pointing at the values used to generate the
     invariant verification state.")
  (targets [invariant]
    "A specter navigator pointing at the values to verify using the
     verification state.")
  (invariant-state [invariant]
    "Initial state for the verification state generation.")
  (invariant-reduce [invariant state source]
    "Updates the verification state using one of the elements reached by
     the specter navigator returned by [[sources]].")
  (invariant-verify [invariant state target]
    "Verify an element reached by the specter navigator returned by [[targets]]
     using the verification state."))

;; ## Collection

(defn- collect-sources
  [invariant data]
  (specter/select (sources invariant) data))

(defn- collect-targets
  [invariant data]
  (specter/select (targets invariant) data))

;; ## Verification

(defn- generate-verification-state
  [invariant data]
  (->> data
       (collect-sources invariant)
       (reduce
         #(invariant-reduce invariant %1 %2)
         (invariant-state invariant))))

(defn- verify-invariants
  [invariant state data]
  (->> (collect-targets invariant data)
       (remove #(invariant-verify invariant state %))))

;; ## Reporting

(defn- report-broken-invariants
  [invariant state broken]
  (when (seq broken)
    (map
      (fn [value]
        (error/invariant-broken
          (invariant-name invariant)
          state
          value))
      broken)))

;; ## Runner

(defn run
  "Run the invariant on the given data, producing a seq of error containers or
   `nil`."
  [invariant data]
  (let [state (generate-verification-state invariant data)
        broken (verify-invariants invariant state data)]
    (report-broken-invariants invariant state broken)))

;; ## Generic Invariant

(defn invariant
  "Generate an invariant describing the relationship between two subsets of
   a given piece of data.

   - `:name`: the name of the invariant,
   - `:sources`: specter paths to elements the invariant state is built from,
   - `:targets`: specter paths to elements that are verified using the invariant,
   - `:state`: the initial invariant state,
   - `:reduce`: a reducer function to be applied to `:state` and `:sources` items,
   - `:verify`: a predicate applied to `:state` and `:targets` items.

   Internally, the incoming data is traversed using `:sources` paths and each
   element passed to `:reduce`. Afterwards, the final state is used to verify
   all `:targets` paths using `:verify`."
  [{:keys [name sources targets state reduce verify]
    :or {targets specter/STAY}}]
  {:pre [(keyword? name) (namespace name)
         sources
         targets
         (fn? reduce)
         (fn? verify)]}
  (reify
    Invariant
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

    clojure.lang.IFn
    (invoke [this data]
      (run this data))))

;; ## Common Invariants

(defn predicate
  "An invariant on the complete piece of data, described by a generic predicate.
   `:predicate` is a single-parameter function processing a seq of elements
   matching the specter navigator given in `:on`.

   For example, to verify that there are at most 5 `:query` operations, one
   could use:

   ```clojure
   (predicate
     {:name :validator/at-most-five-queries
      :on   [:operations ALL (if-path (comp #{:query} :type) STAY)]
      :predicate #(<= (count %) 5)})
   ```
   "
  [{:keys [name on predicate]}]
  {:pre [on (fn? predicate)]}
  (invariant
    {:name    name
     :sources on
     :state   ()
     :reduce  conj
     :verify  (fn [elements _] (predicate elements))}))

(defn unique
  "A uniqueness invariant on all elements matching the specter navigator given
   in `:sources`. `unique-by` can be used to generate the value that has to be
   unique for each element (default: `identity`)."
  [{:keys [name sources unique-by]
    :or {unique-by identity}}]
  (invariant
    {:name    name
     :sources sources
     :targets sources
     :state   {}
     :reduce  #(update %1 (unique-by %2) (fnil inc 0))
     :verify  (fn [fq v]
                (= (fq (unique-by v)) 1))}))
