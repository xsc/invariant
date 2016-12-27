(ns invariant.core.protocols
  "Base Functionality for Invariants"
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
   - `:invariant/values`: the values that could not be verified,
   - `:invariant/error-context`: an arbitrary, invariant-dependent error
     context map.
   "
  ([name path state values]
   (->invariant-error name path state values nil))
  ([name path state values error-context]
   {:pre [(or (nil? error-context)
              (map? error-context))
          (sequential? values)]}
   (cond-> {:invariant/name   name
            :invariant/state  state
            :invariant/path   path
            :invariant/values values}
     error-context (assoc :invariant/error-context error-context))))

;; ## Results

(defn merge-results
  "Merge the results of two invocations of [[run-invariant]]."
  [result1 result2]
  (-> result1
      (update :errors into (:errors result2))))

(defn merge-error-context
  "Merge the given error context into each error of `result`."
  [result error-context]
  {:pre [(or (nil? error-context) (map? error-context))]}
  (if error-context
    (update result
            :errors
            (fn [errors]
              (mapv
                #(update % :invariant/error-context merge error-context)
                errors)))
    result))

(defn invariant-holds
  "Create a result for [[run-invariant]] indicating successful resolution."
  [path state value]
  {:data   [value]
   :path   path
   :state  state
   :errors []})

(defn invariant-failed*
  "Create a result for [[run-invariant]] using a single relevant value and
   a seq of errors."
  [path state value errors]
  {:data   [value]
   :path   path
   :errors errors
   :state  state})

(defn invariant-failed
  "Create a result for [[run-invariant]] indicating failed resolution using
   a single relevant value."
  ([name path state value]
   (invariant-failed name path state value nil))
  ([name path state value error-context]
   (->> [(->invariant-error name path state [value] error-context)]
        (invariant-failed* path state value))))
