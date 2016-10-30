(ns invariant.spec
  (:require [invariant.core :as i]))

(s/def :graphql/document
  (s/and (s/keys :req [:graphql/operations
                       :graphql/fragments])

         ;; Definition Invariant
         (i/invariant
           {;; Name of the invariant
            :name   :validation/fragment-defined?

            ;; Path to match for state generation
            :from   #{[:graphql/fragments :graphql/fragment-name]}

            ;; Path to match for verification
            :to     #{[:graphql/operations :graphql/inline-spread :graphql/fragment-name]}

            ;; Initial State
            :state  #{}

            ;; Reducer Function, will be called on the current `:state` and
            ;; all elements matching `:from`.
            :reduce conj

            ;; Predicate, will be called on the fully reduced `:state` and
            ;; all elements matching `:to`.
            :pred   contains?})

         ;; Uniqueness Invariant
         (i/invariant
           {:name   :validation/fragment-name-unique?
            :from   #{[:graphql/fragments :graphql/fragment-name]}
            :to     #{[:graphql/fragments :graphql/fragment-name]}
            :state  {}
            :reduce #(update %1 %2 (fnil inc 0))
            :pred   (fn [fq n] (= (fq n) 1))})

         ;; Global Invariant (missing `:to`)
         (i/invariant
           {:name   :validation/only-one-anonymous-operation?
            :from   #{[:graphql/operations]}
            :state  {}
            :reduce (fn [state {:keys [graphql/operation-name]}]
                      (if operation-name
                        (assoc state :named? true)
                        (assoc state :anonymous? true)))
            :pred (fn [{:keys [named? anonymous?]} _]
                    (not (and named? anonymous?)))})))

;; Errors like clojure.spec.
;; Maybe use specter for selection.
