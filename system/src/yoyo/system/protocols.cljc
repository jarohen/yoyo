(ns yoyo.system.protocols
  (:require [cats.core :as c]
            [cats.protocols :as cp]))

(declare dependent-monad)

(defrecord Dependency [id dependent])

(defprotocol Dependent
  (dbind [_ f])
  (try-satisfy [_ system]))

(defrecord ResolvedDependent [v]
  cp/Context
  (get-context [_] dependent-monad)

  Dependent
  (dbind [_ f]
    (c/with-monad dependent-monad
      (f v)))

  (try-satisfy [_ _]
    v))

;; Dependent a = Resolved a | Nested Key (System -> Dependent a) | Env (System -> Dependent a)

(defrecord NestedDependent [dep-key f]
  cp/Context
  (get-context [_] dependent-monad)

  Dependent
  (dbind [_ inner-f]
    ;; dbind :: Dependent a -> (a -> Dependent b) -> Dependent b
    ;; inner-f :: a -> Dependent b
    ;; f :: System -> Dependent a
    ;; returns :: Dependent b

    (->NestedDependent dep-key
                       (fn [system]
                         (c/with-monad dependent-monad
                           ;; (f system) :: Dependent a
                           ;; (dbind (f system) inner-f) :: Dependent b
                           (dbind (f system) inner-f)))))

  (try-satisfy [this system]
    (if (contains? system dep-key)
      (f system)
      this)))

(defrecord EnvDependent [f]
  cp/Context
  (get-context [_] dependent-monad)

  Dependent
  (dbind [_ inner-f]
    (->EnvDependent (fn [system]
                      (c/with-monad dependent-monad
                        (dbind (f system) inner-f)))))

  (try-satisfy [this system]
    (try-satisfy (f system) system)))

(defn env-dependent []
  (->EnvDependent (fn [system]
                    (->ResolvedDependent (:env (meta system))))))

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