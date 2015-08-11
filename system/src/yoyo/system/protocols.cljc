(ns yoyo.system.protocols
  (:require [cats.core :as c]
            [cats.protocols :as cp]))

(declare dependent-monad)

(defrecord Dependency [id dependent])

(defprotocol IDependent
  (satisfy [_ system]))

(defrecord Dependent [dep-key f]
  cp/Context
  (get-context [_] dependent-monad)

  IDependent
  (satisfy [dependent system]
    (if (or (nil? dep-key)
            (contains? system dep-key)
            (= dep-key ::!system))
      (f system)
      dependent)))

(def dependent-monad
  (reify
    cp/Functor
    (cp/fmap [_ f dependent]
      (update dependent :f #(comp f %)))

    cp/Monad
    (mreturn [_ v]
      (map->Dependency {:f (constantly v)
                        :dep-key nil}))

    (mbind [_ {outer-dep :dep-key, outer-f :f} f]
      (map->Dependent {:dep-key outer-dep
                       :f (comp f outer-f)}))))
