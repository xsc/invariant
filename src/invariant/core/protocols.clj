(ns invariant.core.protocols
  (:require [invariant.potemkin :refer [defprotocol+]]
            [com.rpl.specter :as specter]))

(defprotocol+ Invariant
  (run-invariant [invariant path state value]
    "Return a map of:

     - `:path`: a descriptive path to the verified data,
     - `:data`: a seq of verified elements,
     - `:state`: the current invariant state,
     - `:errors`: verification errors encountered."))

(defn error
  "Create a new invariant verification error."
  [name path state value]
  {:invariant/name  name
   :invariant/state state
   :invariant/path  path
   :invariant/value value})
