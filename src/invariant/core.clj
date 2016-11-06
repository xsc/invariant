(ns invariant.core
  "API facade providing invariants on Clojure data structures."
  (:refer-clojure :exclude [count and])
  (:require [invariant.core
             [all :refer [->All]]
             [and :refer [->And]]
             [any :refer [->Any]]
             [bind :refer [->Bind]]
             [cycles :refer [->Acyclic]]
             [dependency :refer [->Dependency ->RootDependency]]
             [each :refer [->Each]]
             [fail :refer [->Fail]]
             [fmap :refer [->FMap]]
             [is :refer [->Is]]
             [predicate :refer [->Predicate]]
             [selector :refer [->Selector]]
             [unique :refer [->Unique]]
             [protocols :as p]]
            [com.rpl.specter :as specter]))

(declare each)

;; ## Base Invariants

(defn any
  "An `Invariant` that will never produce an error."
  []
  (->Any))

(defn fail
  "Generate an `Invariant` that will always produce an error."
  [name]
  (->Fail name))

;; ## Selectors

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
  (defn on-current-value
    "An `Invariant` selector pointing at the current (i.e. top-level) value.
     Equivalent to `(on [STAY])` without polluting the path with `STAY`
     elements."
    []
    self-path))

;; ## Predicates

(defn property
  "Generates a predicate whose `pred-fn` will be called with the invariant
   state and the value currently being verified.

   ```clojure
   (-> (invariant/on [:usages ALL :name])
       (invariant/collect-as :declared-variables [:declarations ALL :name])
       (invariant/each
         (invariant/property
           :declared?
           (fn [{:keys [declared-variables]} n]
             (contains? declared-variables n)))))
   ```
   "
  [name pred-fn]
  (->Predicate name pred-fn))

(defn value
  "Generates a _stateless_ predicate whose `pred-fn` will be called with the
   value currently being verified.

   ```clojure
   (-> (invariant/on [:declarations ALL :name])
       (invariant/each
         (invariant/value :prefix-valid? #(string/starts-with? % \"var_\"))))
   ```

   If you need the invariant state to decide on whether the invariant holds,
   use [[property]]."
  [name pred-fn]
  (->Predicate name #(pred-fn %2)))

(defn state
  "Generates a predicate whose `pred-fn` will be called with the current state,
   ignoring any values currently being verified.

   ```clojure
   (-> (invariant/on-current-value)
       (invariant/as :count count)
       (invariant/is?
         (invariant/state :at-least-one? #(pos? (:count %)))))
   ```
   If you need the values being verified to decide on whether the invariant
   holds, use [[property]]."
  [name pred-fn]
  (->Predicate name (fn [state _] (pred-fn state))))

;; ## Conjunction

(defn and
  "Generate an `Invariant` combining all of the given ones.

   ```clojure
   (invariant/and
       (invariant/value :int? (comp integer? :value))
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
               \"F\" (invariant/property :f-args-valid? ...)
               \"G\" (invariant/property :g-args-valid? ...)
               ...)))))
   ```

   This can be used to do invariant dispatch based on concrete values. "
  [bind-fn]
  (->Bind bind-fn))

;; ## Recursive Invariants

(defmacro recursive
  "Generate a recursive invariant bound to `self-sym` within the body.

   ```clojure
   (invariant/recursive
     [self]
     (invariant/and
       (invariant/value :int? (comp integer? :value))
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
  [[self-sym] invariant-form]
  {:pre [(symbol? self-sym)]}
  `(let [promise# (promise)
         ~self-sym (bind (fn [~'_ ~'_] @promise#))
         invariant# ~invariant-form]
     (deliver promise# invariant#)
     ~self-sym))

;; ## Combinators

;; ### Invariant State

(defn as
  "Generates an `Invariant` that uses the given specter path, reduce fn
   and value to attach a key to the invariant state. Note that `as` references
   the input data, not the elements produced by a selector like [[on]].

   ```clojure
   (-> (invariant/on [:usages ALL :name])
       (invariant/as :declared-variables [:declarations ALL :name] conj #{})
       ...)
   ```

   The resulting invariant state can be used e.g. in [[bind]], [[property]]
   and [[state]] and will be shaped similarly to the following:

   ```clojure
   {:declared-variables {\"a\", \"b\", \"c\"}}
   ```

   Alternatively, you can also run a function directly on the input data:

   ```clojure
   (-> (invariant/on [:usages ALL :name])
       (invariant/as :body-count (comp count :body))
       ...)
   ```
   "
  ([invariant state-key path reduce-fn initial-value]
   (let [path (specter/comp-paths path)]
     (->Dependency invariant state-key path reduce-fn initial-value)))
  ([invariant state-key f]
   (->RootDependency invariant state-key f)))

(defn count-as
  "Like [[as]], storing the number of elements at the given specter path
   at the desired key within the invariant state."
  [invariant state-key path]
  (as invariant state-key path (fn [c _] (inc c)) 0))

(defn collect-as
  "Like [[as]], collecting all elements at the given specter path at the
   desired key within the invariant state. If `:unique?` (default: `true`) is
   set the result will be a set."
  [invariant state-key path
   & [{:keys [unique?] :or {unique? true}}]]
  (let [initial-collection (if unique? #{} [])]
    (as invariant state-key path conj initial-collection)))

;; ### Invariant Application

(defn each
  "Generates an `Invariant` that will be individually applied to all elements
   currently being verified."
  [invariant element-invariant]
  (->Each invariant element-invariant))

(defn is?
  "Generates an `Invariant` that will be applied to the single element
   currently being verified. Will throw an exception if a selector like [[on]]
   matched multiple elements.

   ```clojure
   (-> (invariant/on [(must :right)])
       (invariant/is? tree-balanced-invariant))
   ```

   This behaves like [[each]] but will not pollute the invariant error path
   with a zero index."
  [invariant self-invariant]
  (->Is invariant self-invariant))

(defn all
  "Generates an `Invariant` that will be applied to the seq of all elements
   currently being verified."
  [invariant seq-invariant]
  (->All invariant seq-invariant))

;; ### Common Invariants

(defn unique
  "Generates an `Invariant` verifying that all current elements are unique.

   ```clojure
   (-> (invariant/on [:declarations ALL :name])
       (invariant/unique :declarations-unique?))
   ```

   Errors will be reported per-element."
  [invariant name
   & [{:keys [unique-by] :or {unique-by identity}}]]
  (->Unique invariant name unique-by))

(defn acyclic
  "Generates an `Invariant` verifying that there are no cyclic properties
   within the element currently being verified. This needs two functions:

   - `edge-fn`: takes the state and the current value and produces a map of
     node IDs to a set of successor nodes,
   - `describe-fn`: takes the state and the current value and produces a
     function that maps node IDs to a more descriptive representation to
     be included in errors (defaults to returning the node ID itself).

   `edge-fn` should produce a map like the following, describing the edges of
   a graph constructed from the value currently being verified:

   ```clojure
   {:a #{:b :c}
    :c #{:d}
    :d #{:a}}
   ```

   `describe-fn` can be given to provide more information to errors (e.g. to
   retain more of the original input than just the node ID). E.g., if the
   input is a seq of nodes akin to `{:id \"A\", :children #{...}}` one could
   retain the full values using:

   ```clojure
   (defn describe-nodes
     [nodes]
     (into {} (map (juxt :id identity) nodes)))
   ```

   Any error container produced by this invariant will provide the detected
   cycle, as well as the relevant edges within the `:invariant/error` key."
  ([name edge-fn]
   (acyclic name edge-fn (constantly identity)))
  ([name edge-fn describe-fn]
   (->Acyclic name edge-fn describe-fn)))

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
