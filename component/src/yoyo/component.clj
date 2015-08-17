(ns yoyo.component
  (:require [com.stuartsierra.component :as c]
            [yoyo.core :as yc]
            [yoyo.protocols :as yp]))

(defn ->yoyo-component [component]
  (let [started-component (c/start component)]
    (yc/->component started-component
                    (fn []
                      (c/stop started-component)))))

(defn system->yoyo-component [system]
  (let [started-system (c/start-system system)]
    (yc/->component started-system
                    (fn []
                      (c/stop-system started-system)))))

(defrecord YoyoComponent [make-yoyo-component]
  c/Lifecycle
  (start [this]
    (assoc this :component (make-yoyo-component)))

  (stop [{:keys [component] :as this}]
    (when component
      (yp/stop! component))

    (dissoc this :component)))

(comment
  (let [c (->YoyoComponent (fn []
                             (yc/->component :component
                                             (fn []
                                               (println "stopping")))))
        _ (println "created!")
        started-c (c/start c)]

    (println "started:" (:component started-c))
    (c/stop started-c)
    (println "Stopped!")))
