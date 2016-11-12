(ns invariant.debug
  "Debug Mechanisms for Invariants"
  (:require [invariant.core.debug :refer [->Debug]]))

(defn ^:private default-debug-fn
  [k path state value {state' :state, errors :errors}]
  (println "key:    " (pr-str k))
  (println "value:  " (pr-str value))
  (println "at:     " (pr-str path))
  (println "state:  " (pr-str state)
           (if (not= state state')
             (str "-> " (pr-str state'))
             ""))
  (println "errors: " (pr-str (vec errors)))
  (println))

(defn debug
  "Wrap resolution of the given `Invariant` to print debug information to
   stdout.

   ```clojure
   (-> (invariant/on-current-value)
       (invariant/is?
         (debug ::predicate (invariant/value :int? integer?))))
   ```

   An explicit `debug-fn` can be given."
  ([k invariant]
   (debug k invariant default-debug-fn))
  ([k invariant debug-fn]
   (->Debug k invariant debug-fn)))
