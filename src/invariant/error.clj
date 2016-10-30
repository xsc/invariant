(ns invariant.error
  "Error containers for broken invariants.")

(defrecord InvariantBroken [invariant-name state value])
(alter-meta! #'->InvariantBroken assoc :private true)
(alter-meta! #'map->InvariantBroken assoc :private true)

(defn broken-invariant
  "Generate a value describing a broken invariant."
  [invariant-name state value]
  (->InvariantBroken invariant-name state value))
