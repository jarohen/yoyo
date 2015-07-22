(ns yoyo.component
  (:require [com.stuartsierra.component :as c]
            [yoyo.core :as yc]))

(defn with-component [component]
  (fn [f]
    (let [started-component (c/start component)]
      (f started-component
         (fn []
           (c/stop started-component))))))

(defn with-component-system [system]
  (fn [f]
    (let [started-system (c/start-system system)]
      (f started-system
         (fn []
           (c/stop-system started-system))))))

(defrecord YoyoComponent [yoyo-component]
  c/Lifecycle
  (start [this]
    (let [!component (promise)
          stop! (yc/run-system (fn [f]
                                 (yoyo-component (fn [component stop!]
                                                   (deliver !component component)
                                                   (f component stop!)))))]
      (assoc this
        :component @!component
        ::stop! stop!)))

  (stop [{:keys [component stop!] :as this}]
    (when stop!
      (stop!))

    (dissoc this :component ::stop!)))

(comment
  (let [c (->YoyoComponent (fn component [f]
                             (f :component
                                (fn []
                                  (println "stopping")))))
        _ (println "created!")
        started-c (c/start c)]

    (println "started:" (:component started-c))
    (c/stop started-c)
    (println "Stopped!")))
