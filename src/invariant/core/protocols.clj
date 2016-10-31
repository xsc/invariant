(ns invariant.core.protocols
  (:require [invariant.potemkin :refer [defprotocol+]]
            [com.rpl.specter :as specter]))

(defprotocol+ Invariant
  (run-invariant [invariant state value]
    "Return a map of `:data`, `:errors` and `:state`."))

(defrecord InvariantError [name state value])
