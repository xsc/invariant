(ns ^:no-doc invariant.core.engine
  (:require [invariant.core.protocols :as p]
            [com.rpl.specter :as specter]))

;; ## Collection

(defn- collect-sources
  [invariant data]
  (specter/traverse (p/sources invariant) data))

(defn- collect-targets
  [invariant data]
  (specter/select (p/targets invariant) data))

;; ## Verification

(defn- generate-verification-state
  [invariant data]
  (->> data
       (collect-sources invariant)
       (reduce
         #(p/invariant-reduce invariant %1 %2)
         (p/invariant-state invariant))))

(defn- verify-invariants
  [invariant state data]
  (->> (collect-targets invariant data)
       (remove #(p/invariant-verify invariant state %))))

;; ## Reporting

(defn- report-broken-invariants
  [invariant state broken]
  (when (seq broken)
    (map
      (fn [value]
        (p/report-broken-invariant invariant state value))
      broken)))

;; ## Runner

(defn run
  "Run the invariant on the given data, producing a seq of error containers or
   `nil`.

   See [[debug]] for a more verbose result."
  [invariant data]
  (let [state (generate-verification-state invariant data)
        broken (verify-invariants invariant state data)]
    (report-broken-invariants invariant state broken)))

;; ## Debug Runner

(defn debug
  "Run the invariant on the given data, producing a map of:

   - `:result` as produced by [[run]],
   - `:sources` as collected from the data,
   - `:targets` as collected from the data,
   - `:state` as computed from the sources.

   Should be used for debugging only, otherwise [[run]] is preferred."
  [invariant data]
  (let [sources (into [] (collect-sources invariant data))
        targets (into [] (collect-targets invariant data))
        state (generate-verification-state invariant data)
        broken (verify-invariants invariant state data)]
    {:result (report-broken-invariants invariant state broken)
     :sources sources
     :targets targets
     :state   state}))
