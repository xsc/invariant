(ns invariant.core.selector
  (:require [invariant.core.protocols :refer :all]
            [com.rpl.specter :as specter]))

(deftype Selector [path path-form]
  Invariant
  (run-invariant [_ path' state value]
    {:data   (into [] (specter/traverse path value))
     :path   (into path' path-form)
     :state  state
     :errors []}))
