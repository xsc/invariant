(ns invariant.utils.tarjan)

(def initial-env
  {:indices {}
   :stack ()
   :strongly-connected-components []})

(defn- push-node
  [{:keys [stack] :as env} node]
  (-> env
      (assoc-in [:indices node] (count stack))
      (update :stack conj node)))

(defn- seen?
  [{:keys [indices]} node]
  (contains? indices node))

(defn- node-index
  [{:keys [indices]} node]
  (get indices node))

(defn- merge-successor-index
  [env node successor]
  (if-let [n (node-index env successor)]
    (update-in env [:indices node] min n)
    env))

(defn- recur-successors
  [step-fn env node successors]
  (let [n (node-index env node)]
    (reduce
      (fn [env successor]
        (-> (step-fn env successor)
            (merge-successor-index node successor)))
      env successors)))

(defn- strongly-connected?
  [env node index]
  (= (node-index env node) index))

(defn- take-strongly-connected-components
  [{:keys [stack]} push-index]
  (set (take (- (count stack) push-index) stack)))

(defn- reset-indices
  [env nodes]
  (let [assoc-nil #(assoc %1 %2 nil)]
    (update env :indices #(reduce assoc-nil % nodes))))

(defn- reset-stack
  [env stack]
  (assoc env :stack stack))

(defn- finalize
  [env nodes]
  (update env :strongly-connected-components conj nodes))

(defn- update-strongly-connected-components
  [node->successors env node]
  (if-not (seen? env node)
    (let [step-fn #(update-strongly-connected-components node->successors %1 %2)
          stack   (:stack env)
          env     (push-node env node)
          index   (node-index env node)
          succs   (node->successors node)
          env     (recur-successors step-fn env node succs)]
      (if (strongly-connected? env node index)
        (let [sccs (take-strongly-connected-components env index)]
          (-> env
              (reset-indices sccs)
              (reset-stack stack)
              (finalize sccs)))
        env))
    env))

(defn strongly-connected-components
  "Find all strongly connected components within the given directed graph using
   Tarjan's SCC algorithm.

   Inspired by: https://gist.github.com/cgrand/5188919"
  [node->successors nodes]
  (:strongly-connected-components
    (reduce
      #(update-strongly-connected-components node->successors %1 %2)
      initial-env
      nodes)))
