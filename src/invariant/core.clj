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

(declare each)

;; ## Base Invariants

(defn on*
  "Generates an `Invariant` that uses the given specter path to collect
   all pieces of data the invariant should apply to. This is basically a
   _selector_, producing elements subsequent invariants will be applied to.

   ```clojure
   (-> (invariant/on* [:declarations ALL] '[:declarations ALL])
       (invariant/each ...))
   ```

   [[on]] should be preferred since it will automatically generate `path-form`
   for you."
  [path path-form]
  {:pre [(sequential? path-form)]}
  (let [path (specter/comp-paths path)]
    (->Selector path path-form)))

(defmacro on
  "Generates an `Invariant` that uses the given specter path to collect
   all pieces of data the invariant should apply to. This is basically a
   _selector_, producing elements subsequent invariants will be applied to.

   ```clojure
   (-> (invariant/on [:declarations ALL])
       (invariant/each ...))
   ```

   If you need to generate a selector dynamically or from a path stored within
   a var/binding, use [[on*]]."
  [path]
  `(on* ~path (quote ~path)))

(let [self-path (on* specter/STAY [])]
  (defn current-value
    "An `Invariant` selector pointing at the current (i.e. top-level) value.
     Equivalent to `(on [STAY])` without polluting the path with `STAY`
     elements."
    []
    self-path))

(defn predicate
  "Generates a predicate whose `pred-fn` will be called with the invariant
   state and the value currently being verified.

   ```clojure
   (-> (invariant/on [:usages ALL :name])
       (invariant/collect :declared-variables [:declarations ALL :name])
       (invariant/each
         (invariant/predicate
           :declared?
           (fn [{:keys [declared-variables]} n]
             (contains? declared-variables n)))))
   ```

   If `invariant` is given, the predicate will directly be attached to it
   using [[each]]."
  ([name pred-fn]
   (->Predicate name pred-fn))
  ([invariant name pred-fn]
   (each invariant (predicate name pred-fn))))

(defn property
  "Generates a _stateless_ predicate whose `pred-fn` will be called with the
   value currently being verified, as well as the given arguments.

   ```clojure
   (-> (invariant/on [:declarations ALL :name])
       (invariant/each
         (invariant/property :prefix-valid? string/starts-with? \"var_\")))
   ```

   If you need the invariant state to decide on whether the invariant holds,
   use [[predicate]]."
  [name pred-fn & args]
  (->Predicate name
               (if (seq args)
                 #(apply pred-fn %2 args)
                 #(pred-fn %2))))

(def any
  "An `Invariant` that will never produce an error."
  (->Any))

(defn fail
  "Generate an `Invariant` that will always produce an error."
  [name]
  (->Fail name))

(defn and
  "Generate an `Invariant` combining all of the given ones.

   ```clojure
   (invariant/and
       (invariant/property :value-int? (comp integer? :value))
       (-> (invariant/on [:children ALL])
           (invariant/each ...)))
   ```
   "
  [invariant & more]
  (if (seq more)
    (->And (cons invariant more))
    invariant))

;; ## fMap/bind

(defn fmap
  "Transform each element currently being verified.

   ```clojure
   (-> (invariant/on [:declarations ALL :name]
       (invariant/fmap
         #(assoc % :name-count (count (:name %))))))
   ```
   "
  [invariant f]
  (->FMap invariant f))

(defn bind
  "Generate an `Invariant` that will use `bind-fn`, applied to the invariant
   state and the value currently being verified, to decide on an invariant
   to use.

   ```clojure
   (-> (invariant/on [:functions ALL])
       (invariant/each
         (invariant/bind
           (fn [_ {:keys [function-name]}]
             (case function-name
               \"F\" (invariant/predicate :f-args-valid? ...)
               \"G\" (invariant/predicate :g-args-valid? ...)
               ...)))))
   ```

   This can be used to do invariant dispatch based on concrete values.

   If `invariant` is given, the `bind` logic will directly be attached to it
   using [[each]]."
  ([bind-fn]
   (->Bind bind-fn))
  ([invariant bind-fn]
   (each invariant (bind bind-fn))))

;; ## Recursive Invariants

(defmacro recursive
  "Generate a recursive invariant bound to `self-sym` within the body.

   ```clojure
   (invariant/recursive
     [self]
     (invariant/and
       (invariant/property :value-int? (comp integer? :value))
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

;; ### Invariant State

(defn with
  "Generates an `Invariant` that uses the given specter path, reduce fn
   and value to attach a key to the invariant state.

   ```clojure
   (-> (invariant/on [:usages ALL :name])
       (invariant/with :declared-variables [:declarations ALL :name] conj #{})
       ...)
   ```

   The resulting invariant state can be used e.g. in [[bind]] and [[predicate]]
   and will be shaped similarly to the following:

   ```clojure
   {:declared-variables {\"a\", \"b\", \"c\"}}
   ```

   NOTE: `with` runs on the input data, not on the elements produced by a
   selector like [[on]]."
  [invariant state-key path reduce-fn initial-value]
  (let [path (specter/comp-paths path)]
    (->Dependency invariant state-key path reduce-fn initial-value)))

(defn with-value
  "Like [[with]], storing the value generated by `f` at the desired key within
   the invariant state."
  [invariant state-key f]
  (with invariant state-key [] #(f %2)))

(defn with-count
  "Like [[with]], storing the number of elements at the given specter path
   at the desired key within the invariant state."
  [invariant state-key path]
  (with invariant state-key path (fn [c _] (inc c)) 0))

;; ### Invariant Application

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

(defn holds?
  "Returns `true` if running the invariant against the given data does not
   produce any errors."
  ([invariant data]
   (holds? invariant {} data))
  ([invariant initial-state data]
   (->> data
        (p/run-invariant invariant [] initial-state)
        (:errors)
        (empty?))))
