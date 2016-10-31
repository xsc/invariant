(ns invariant.core.error)

(defrecord InvariantError [name state value])
