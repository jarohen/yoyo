(ns yoyo.resource
  (:refer-clojure :exclude [with-open])
  (:require [yoyo.resource.protocols :as p]
            [cats.core :as c]
            [cats.protocols :as cp]))

(def with p/with)
(alter-meta! #'with (constantly (meta #'p/with)))

(defn with-open [o]
  (with o (fn [] (.close o))))
