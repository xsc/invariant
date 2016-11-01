(ns invariant.spec
  (:require [invariant.core :as invariant]
            [invariant.potemkin :refer [import-vars]]
            [clojure.spec.gen :as gen]
            [clojure.spec :as s]))

;; ## Generic Invariant Spec

(defn ^:no-doc holds-spec-impl
  "Do not use – use `holds?` instead."
  [spec invariants forms gfn]
  (reify
    s/Specize
    (specize* [s] s)
    (specize* [s _] s)

    s/Spec
    (conform* [_ x]
      (let [v (s/conform* (s/specize* spec) x)]
        (if (or (s/invalid? v)
                (some #(not (invariant/holds? % v)) invariants))
          ::s/invalid
          v)))
    (unform* [_ x]
      (s/unform spec x))
    (explain* [_ path via in x]
      (or (s/explain* (s/specize* spec) path via in x)
          (->> invariants
               (mapcat #(invariant/check % x))
               (mapv
                 (fn [error]
                   {:path (into (vec path) (:invariant/path error))
                    :pred (list 'invariant/holds? (:invariant/name error) '%)
                    :val  (:invariant/value error)
                    :via  via
                    :in   in})))))
    (gen* [_ overrides path rmap]
      (if gfn
        (gfn)
        (let [base-gen (s/gen spec)]
          ;; TODO: Build a Generator from Invariants
          base-gen)))
    (with-gen* [_ gfn]
      (holds-spec-impl spec invariants forms gfn))
    (describe* [_]
      `(invariant/holds? ~spec ~@forms))))

;; ## Public Macro

(defmacro holds?
  "Returns a spec for data conforming to both `data-spec` and the given
   invariants."
  [data-spec & invariants]
  `(holds-spec-impl ~data-spec [~@invariants] (quote ~invariants) nil))
