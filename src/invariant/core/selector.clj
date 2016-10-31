(ns invariant.core.selector
  (:require [invariant.core.protocols :refer :all]
            [com.rpl.specter :as specter]))

(deftype Selector [path]
  Invariant
  (run-invariant [_ state value]
    {:data   (into [] (specter/traverse path value))
     :state  state
     :errors []}))
