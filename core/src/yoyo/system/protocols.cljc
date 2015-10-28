(ns yoyo.system.protocols
  (:require [cats.core :as c]
            [cats.protocols :as cp]))

(defprotocol IDependency
  (get-dependent [_]))

(defrecord InitialDependency [id dependent-fn]
  IDependency
  (get-dependent [_]
    (dependent-fn)))

(defrecord Dependency [id dependent]
  IDependency
  (get-dependent [_]
    dependent))
