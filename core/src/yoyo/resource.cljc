(ns yoyo.resource
  (:refer-clojure :exclude [with-open])
  (:require [yoyo.resource.protocols :as p]
            [yoyo.resource.sink :as sink]
            [cats.core :as c]
            [cats.context :as ctx]
            [cats.protocols :as cp]
            [clojure.java.io :as io])
  (:import [yoyo.resource.protocols Resource]))

(defn ->resource
  " :: a -> Component a

  Like `return`. Takes the value of the component and, optionally, a
  function to stop the component."

  ([v]
   (->resource v (fn [])))

  ([v close!]
   (p/->Resource v close!)))

(def resource-monad
  (reify cp/Context
    (-get-level [_]
      ctx/+level-default+)

    cp/Functor
    (-fmap [m f {outer-v :v, close-outer! :close!}]
      (->resource (f outer-v) close-outer!))

    cp/Monad
    (-mreturn [_ v]
      (->resource v nil))

    (-mbind [_ {outer-v :v, close-outer! :close!} f]
      (let [{inner-v :v, close-inner! :close!} (try
                                                 (f outer-v)
                                                 (catch #?(:clj Throwable, :cljs js/Error) t
                                                   (try
                                                     (when close-outer!
                                                       (close-outer!))

                                                     (finally
                                                       (throw t)))))]
        (->resource inner-v
                    (cond
                      (and close-inner! close-outer!) (fn []
                                                        (try
                                                          (when close-inner!
                                                            (close-inner!))

                                                          (finally
                                                            (when close-outer!
                                                              (close-outer!)))))
                      close-outer! close-outer!
                      close-inner! close-inner!))))))

(defrecord ResourceT [inner-m v])

(defn resource-monad-t [inner-m]
  (reify cp/Context
    (-get-level [_]
      ctx/+level-transformer+)

    cp/Functor
    (-fmap [m f mv]
      (cp/-mbind m mv (comp #(cp/-mreturn m %) f)))

    cp/Monad
    (-mreturn [_ v]
      (->ResourceT inner-m (cp/-mreturn inner-m (cp/-mreturn resource-monad v))))

    (-mbind [_ mv f]
      (->ResourceT inner-m
                   (cp/-mbind inner-m (:v mv)
                              (fn [resource]
                                (cp/-mreturn inner-m (cp/-mbind resource-monad resource f))))))

    cp/MonadTrans
    (-lift [m mv]
      (->ResourceT inner-m
                   (cp/-mbind inner-m mv
                              (fn [inner-val]
                                (cp/-mreturn inner-m (cp/-mreturn resource-monad inner-val))))))))

(extend-protocol cp/Contextual
  Resource
  (-get-context [_]
    resource-monad)

  ResourceT
  (-get-context [{:keys [inner-m]}]
    (resource-monad-t inner-m)))

(defn with-resource
  "Given a started resource (simple or composite), calls the provided
  1-arg function with the resource's value and, when the function's
  done, stops the resource."
  [resource f]

  (try
    (f (:v resource))
    (finally
      (p/-close! resource))))

(defn with-open [^java.io.Closeable o]
  (->resource o (fn [] (.close o))))

(defn with-system-put-to
  "Given a running system, stores the value of the system in the given
   sink (either an atom or (CLJ only) a symbol, like 'user/foo-system).

   When the system stops, it will clear the sink."
   [system sink]

   (c/mlet [running-system system]
     (sink/set-system! sink running-system)
     (->resource running-system
                 (fn []
                   (sink/set-system! sink nil)))))
