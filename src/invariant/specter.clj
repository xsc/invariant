(ns invariant.specter
  "A collection of [specter][1] navigators for common requirements related to
   invariant verification.

   [1]: https://github.com/nathanmarz/specter"
  (:require [com.rpl.specter :as specter]))

(def ^{:arglists '([k])} dfs
  "Specter navigator finding all occurences of a given path within a
   piece of data. Uses depth-first-search to traverse the full tree."
  (specter/recursive-path
    [inner-path]
    p
    (specter/cond-path
      map?  (specter/multi-path inner-path [specter/MAP-VALS p])
      coll? (specter/multi-path inner-path [specter/ALL p])
      specter/STAY)))

(defn dfs-vals
  "Like [[dfs]], finding all values for the given key."
  [k]
  (dfs (specter/must k)))
