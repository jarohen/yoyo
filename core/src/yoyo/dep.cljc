(ns yoyo.dep
  (:require [yoyo.dep.protocols :as p]
            [yoyo.dep.run :as r]
            [cats.protocols :as cp]
            [clojure.walk :refer [postwalk]]))

(defn ->dep
  " :: a -> Dependent a

  `return`, for the Dependent monad"
  [v]

  (p/->ResolvedDependent v))

(defn ask
  " :: path -> Dependent a

  Given a path into the system map, returns a Dependent awaiting the given path.

  Essentially, behaves as if it calls `get-in` on the system with the
  provided path.

  Usage:

    (assuming a system with a dependency named `:config` - a map
     containing {:s3 {:creds ..., :bucket \"...\"}})

    (defn m-get-object [s3-path]
      (c/mlet [{:keys [creds bucket]} (yd/ask :config :s3)]
        (yd/->dep
         (aws.sdk.s3/get-object creds bucket s3-path))))"

  [p & path]

  (p/map->NestedDependent {:dep-key p
                           :f (fn [system]
                                (->dep (get-in system (cons p path))))}))

(-> (def run r/run)
    (alter-meta! (constantly (meta #'r/run))))

(def dependent-monad
  (reify
    cp/Functor
    (-fmap [m f dependent]
      (cp/-mbind m dependent (comp #(cp/-mreturn m %) f)))

    cp/Monad
    (-mreturn [_ v]
      (->dep v))

    (-mbind [_ dependent f]
      (p/dbind dependent f))))

(defn <!! [dependent]
  (throw (ex-info "<!! used outside of mgo!"
                  {:dependent dependent})))

(defn <ch [dependent]
  (throw (ex-info "<ch used outside of mgo!"
                  {:dependent dependent})))

#?(:clj
   (defmacro mgo [& body]
     (let [ys-env-sym (gensym "ys-env")]
       `(c/bind (p/env-dependent)
                (fn [~ys-env-sym]
                  ~(postwalk (fn [o]
                               (if (symbol? o)
                                 (condp = (when-let [v (resolve &env o)]
                                            (let [{sym-name :name, sym-ns :ns} (meta v)]
                                              (symbol (str sym-ns) (str sym-name))))
                                   'yoyo.system/<!! `#(r/run!! % ~ys-env-sym)
                                   'yoyo.system/<ch `#(r/run-async % ~ys-env-sym)
                                   o)
                                 o))

                             `(do ~@body)))))))
