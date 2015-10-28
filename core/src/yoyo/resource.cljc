(ns yoyo.resource
  (:require [yoyo.resource.protocols :as p]
            [yoyo.resource.sink :as sink]
            [cats.core :as c]
            [cats.protocols :as cp])
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
  (reify
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

(extend-protocol cp/Contextual
  Resource
  (-get-context [_]
    resource-monad))

(defn with-resource
  "Given a started resource (simple or composite), calls the provided
  1-arg function with the resource's value and, when the function's
  done, stops the resource."
  [resource f]

  (try
    (f (:v resource))
    (finally
      (p/-close! resource))))

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
