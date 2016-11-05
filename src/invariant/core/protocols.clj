(ns invariant.core.protocols
  (:require [invariant.potemkin :refer [defprotocol+]]
            [com.rpl.specter :as specter]))

;; ## Protocol

(defprotocol+ Invariant
  (run-invariant [invariant path state value]
    "Return a map of:

     - `:path`: a descriptive path to the verified data,
     - `:data`: a seq of verified elements,
     - `:state`: the current invariant state,
     - `:errors`: verification errors encountered."))

;; ## Error Container

(defn ->invariant-error
  "Create a new invariant verification error, a map with the following keys:

   - `:invariant/name`: the name of the failed invariant,
   - `:invariant/state`: the state of the invariant when failing,
   - `:invariant/path`: the path the invariant failed at,
   - `:invariant/value`: the value that could not be verified,
   - `:invariant/error`: arbitrary, invariant-dependent error information.
   "
  ([name path state value]
   (->invariant-error name path state value nil))
  ([name path state value error]
   (cond-> {:invariant/name  name
            :invariant/state state
            :invariant/path  path
            :invariant/value value}
     error (assoc :invariant/error error))))

;; ## Results

(defn merge-results
  "Merge the results of two invocations of [[run-invariant]]."
  [result1 result2]
  (-> result1
      (update :errors into (:errors result2))))

(defn invariant-holds
  "Create a result for [[run-invariant]] indicating successful resolution."
  [path state value]
  {:value  [value]
   :path   path
   :state  state
   :errors []})

(defn invariant-failed*
  [name path state value errors]
  {:value  [value]
   :path   path
   :errors errors
   :state  state})

(defn invariant-failed
  "Create a result for [[run-invariant]] indicating failed resolution."
  ([name path state value]
   (invariant-failed name path state value nil))
  ([name path state value error-data]
   (->> [(->invariant-error name path state value error-data)]
        (invariant-failed* name path state value))))
