(ns invariant.core
  "Invariants on Clojure data structures."
  (:refer-clojure :exclude [count and])
  (:require [invariant.core
             and
             count
             engine
             invariant
             unique]
            [invariant.potemkin :refer [import-vars]]))

(import-vars
  [invariant.core.engine
   debug
   run]
  [invariant.core.and
   and*
   and]
  [invariant.core.count
   count]
  [invariant.core.invariant
   invariant]
  [invariant.core.unique
   unique])
