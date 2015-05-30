(ns yoyo.component
  (:require [com.stuartsierra.component :as c]))

(defn with-component [component f]
  (let [started-component (c/start component)]
    (try

      (f started-component)

      (finally
        (c/stop started-component)))))

(defn with-component-system [system f]
  (let [started-system (c/start-system system)]
    (try

      (f started-system)

      (finally
        (c/stop-system started-system)))))
