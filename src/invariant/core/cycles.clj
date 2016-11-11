(ns invariant.core.cycles
  (:require [invariant.core.protocols :refer :all]
            [invariant.utils.tarjan :as tarjan]
            [clojure.set :as set]))

;; ## Cycle Detection

(defn- find-cycles
  "Uses Tarjan's SCC algorithm to detect cycles within the given directed
   graph. Additionally, self-referencing items will be returned."
  [node->successors]
  (let [nodes (keys node->successors)
        sccs  (tarjan/strongly-connected-components node->successors nodes)]
    (seq
      (keep
        (fn [scc]
          (if (= (count scc) 1)
            (let [node (first scc)]
              (when (some-> (node->successors node)
                            (contains? node))
                scc))
            scc))
        sccs))))

(defn- cycle->error
  "Generate an error container for a detected cycle. Includes the node IDs
   that make up the cycle, the edges involved, as well as the original elements
   responsible."
  [name path state node->successors cycle elements]
  (let [edges (->> (for [[k v] node->successors
                             :when (contains? cycle k)]
                         [k (set/intersection v cycle)])
                       (into {}))
        error-data {:invariant/cycle cycle
                    :invariant/edges edges}]
    (->invariant-error name path state elements error-data)))

;; ## Type

(deftype Acyclic [name edge-fn describe-fn]
  Invariant
  (run-invariant [_ path state value]
    (let [node->successors (edge-fn state value)]
      (if-let [cycles (find-cycles node->successors)]
        (let [node->element (describe-fn state value)]
          (->> (for [cycle cycles
                     :let [elements (mapv node->element cycle)]]
                 (cycle->error name path state node->successors cycle elements))
               (invariant-failed* name path state value)))
        (invariant-holds path state value)))))
