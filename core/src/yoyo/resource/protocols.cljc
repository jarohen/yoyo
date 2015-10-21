(ns yoyo.resource.protocols
  (:require [cats.core :as c]
            [cats.protocols :as cp]))

(defprotocol Resource
  (-resource [_])
  (-close! [_])
  (-run! [_ f]))

(declare resource)

(defrecord CloseableResource [o close! !used?]
  Resource
  (-resource [_] o)

  (-close! [_]
    (when (compare-and-set! !used? false true)
      (close!)))

  (-run! [this f]
    (when-not @!used?

      (try
        (f o)
        (finally
          (-close! this)))))

  cp/Context
  (get-context [_]
    resource))

(defn with
  ([o]
   (with o (fn [])))

  ([o close!]
   (map->CloseableResource {:o o
                            :close! close!
                            :!used? (atom false)})))

(def resource
  (reify
   cp/Functor
   (fmap [m f mv]
         (cp/mbind m mv (comp #(cp/mreturn m %) f)))

   cp/Monad
   (mbind [_ outer f]
          (try
            (let [inner (f (-resource outer))]
              (with inner
                    (fn []
                      (try
                        (-close! inner)
                        (finally
                          (-close! outer))))))

            (catch Throwable t
              (-close! outer))))

   (mreturn [_ v]
            (with v))))
