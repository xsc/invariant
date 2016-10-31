(ns invariant.core
  "API facade providing invariants on Clojure data structures."
  (:refer-clojure :exclude [count and])
  (:require [invariant.core
             [all :refer [->All]]
             [any :refer [->Any]]
             [bind :refer [->Bind]]
             [dependency :refer [->Dependency]]
             [each :refer [->Each]]
             [fail :refer [->Fail]]
             [predicate :refer [->Predicate]]
             [selector :refer [->Selector]]
             [protocols :as p]]))

;; ## Base Invariants

(defn on
  "Generates an `Invariant` that uses the given specter path to collect
   all pieces of data the invariant should apply to."
  [path]
  (->Selector path))

(defn predicate
  "Generates a predicate whose `pred-fn` will be called with the invariant
   state and the value currently being verified."
  [name pred-fn]
  (->Predicate name pred-fn))

(def any
  "An `Invariant` that will never produce an error."
  (->Any))

(defn fail
  "Generate an `Invariant` that will always produce an error."
  [name]
  (->Fail name))

(defn bind
  "Generate an `Invariant` that will use `bind-fn`, applied to the invariant
   state and the value currently being verified, to decide on an invariant
   to use.

   This can be used to do invariant dispatch based on concrete values."
  [bind-fn]
  (->Bind bind-fn))

;; ## Combinators

(defn with
  "Generates an `Invariant` that uses the given specter path, reduce fn
   and value to attach a key to the invariant state."
  [invariant state-key path reduce-fn initial-value]
  (->Dependency invariant state-key path reduce-fn initial-value))

(defn each
  "Generates an `Invariant` that will be individually applied to all elements
   currently being verified."
  [invariant element-invariant]
  (->Each invariant element-invariant))

(defn all
  "Generates an `Invariant` that will be applied to the seq of all elements
   currently being verified."
  [invariant seq-invariant]
  (->All invariant seq-invariant))

;; ## Run Function

(defn run
  "Verify that the given invariant holds for the given piece of data."
  ([invariant data]
   (p/run-invariant invariant {} data))
  ([invariant initial-state data]
   (p/run-invariant invariant initial-state data)))

(defn check
  "Like [[run]] but will return either `nil` or a seq of errors."
  ([invariant data]
   (check invariant {} data))
  ([invariant initial-state data]
   (->> (p/run-invariant invariant initial-state data)
        (:errors)
        (seq))))
