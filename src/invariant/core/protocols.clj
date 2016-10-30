(ns invariant.core.protocols
  (:require [invariant.potemkin :refer [defprotocol+]]))

(defprotocol+ Invariant
  (invariant-name [invariant]
    "The name of the invariant, given as a namespaced keyword.")
  (sources [invariant]
    "A specter navigator pointing at the values used to generate the
     invariant verification state.")
  (targets [invariant]
    "A specter navigator pointing at the values to verify using the
     verification state.")
  (invariant-state [invariant]
    "Initial state for the verification state generation.")
  (invariant-reduce [invariant state source]
    "Updates the verification state using one of the elements reached by
     the specter navigator returned by [[sources]].")
  (invariant-verify [invariant state target]
    "Verify an element reached by the specter navigator returned by [[targets]]
     using the verification state.")
  (report-broken-invariant [invariant state broken]
    "Create a value describing a broken invariant."))
