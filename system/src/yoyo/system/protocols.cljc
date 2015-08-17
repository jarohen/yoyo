(ns yoyo.system.protocols
  (:require [cats.core :as c]
            [cats.protocols :as cp]))

(declare dependent-monad)

(defrecord Dependency [id dependent])

(defprotocol Dependent
  (dbind [_ f]))

(defrecord ResolvedDependent [v]
  cp/Context
  (get-context [_] dependent-monad)

  Dependent
  (dbind [_ f]
    (c/with-monad dependent-monad
      (f v))))

;; Dependent a = Resolved a | Nested Key (Env -> Dependent a)

(defrecord NestedDependent [dep-key f]
  cp/Context
  (get-context [_] dependent-monad)

  Dependent
  (dbind [_ inner-f]
    ;; dbind :: Dependent a -> (a -> Dependent b) -> Dependent b
    ;; inner-f :: a -> Dependent b
    ;; f :: Env -> Dependent a
    ;; returns :: Dependent b

    (->NestedDependent dep-key
                       (fn [system]
                         (c/with-monad dependent-monad
                           ;; (f system) :: Dependent a
                           ;; (dbind (f system) inner-f) :: Dependent b
                           (dbind (f system) inner-f))))))

(def dependent-monad
  (reify
    cp/Functor
    (cp/fmap [m f dependent]
      (cp/mbind m dependent (comp #(cp/mreturn m %) f)))

    cp/Monad
    (mreturn [_ v]
      (->ResolvedDependent v))

    (mbind [_ dependent f]
      (dbind dependent f))))
