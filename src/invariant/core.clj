(ns invariant.core
  "API facade providing invariants on Clojure data structures."
  (:refer-clojure :exclude [count and])
  (:require [invariant.core
             [all :refer [->All]]
             [and :refer [->And]]
             [any :refer [->Any]]
             [bind :refer [->Bind]]
             [dependency :refer [->Dependency]]
             [each :refer [->Each]]
             [fail :refer [->Fail]]
             [fmap :refer [->FMap]]
             [predicate :refer [->Predicate]]
             [selector :refer [->Selector]]
             [protocols :as p]]
            [com.rpl.specter :as specter]))

;; ## Base Invariants

(defn on*
  "Generates an `Invariant` that uses the given specter path to collect
   all pieces of data the invariant should apply to.

   [[on]] should be preferred since it will auomatitcally generate `path-form`
   for you."
  [path path-form]
  (let [path (specter/comp-paths path)]
    (->Selector path path-form)))

(defmacro on
  "Generates an `Invariant` that uses the given specter path to collect
   all pieces of data the invariant should apply to."
  [path]
  `(on* ~path (quote ~path)))

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

(defn and
  "Generate an `Invariant` combining all of the given ones."
  [invariant & more]
  (if (seq more)
    (->And (cons invariant more))
    invariant))

;; ## fMap/bind

(defn fmap
  "Transform each element currently being verified"
  [invariant f]
  (->FMap invariant f))

(defn bind
  "Generate an `Invariant` that will use `bind-fn`, applied to the invariant
   state and the value currently being verified, to decide on an invariant
   to use.

   This can be used to do invariant dispatch based on concrete values."
  [bind-fn]
  (->Bind bind-fn))

;; ## Recursive Invariants

(defmacro recursive
  "Generate a recursive invariant bound to `self-sym` within the body.

   ```clojure
   (invariant/recursive
     [self]
     (invariant/and
       (invariant/predicate :value-int? #(integer? (:value %2)))
       (-> (invariant/on [:children ALL])
           (invariant/each self))))
   ```

   The above could be used to verify e.g. the following nested map:

   ```clojure
   {:value 1
    :children [{:value :x
                :children [{:value 4}]}]}
   ```
   "
  [[self-sym] & body]
  {:pre [(symbol? self-sym)]}
  `(let [promise# (promise)
         ~self-sym (bind (fn [~'_ ~'_] @promise#))
         invariant# ~@body]
     (deliver promise# invariant#)
     invariant#))

;; ## Combinators

(defn with
  "Generates an `Invariant` that uses the given specter path, reduce fn
   and value to attach a key to the invariant state."
  [invariant state-key path reduce-fn & [initial-value]]
  (let [reduce-fn (if initial-value
                    (fnil reduce-fn initial-value)
                    reduce-fn)
        path (specter/comp-paths path)]
    (->Dependency invariant state-key path reduce-fn)))

(defn with-count
  "Like [[with]], storing the number of elements at the given specter path
   at the desired key within the invariant state."
  [invariant state-key path]
  (with invariant state-key path (fn [c _] (inc c)) 0))

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
   (run invariant {} data))
  ([invariant initial-state data]
   (p/run-invariant invariant [] initial-state data)))

(defn check
  "Like [[run]] but will return either `nil` or a seq of errors."
  ([invariant data]
   (check invariant {} data))
  ([invariant initial-state data]
   (->> data
        (p/run-invariant invariant [] initial-state)
        (:errors)
        (seq))))
