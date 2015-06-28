(ns yoyo.component
  (:require [com.stuartsierra.component :as c]
            [yoyo :as y]))

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

(defrecord YoyoComponent [make-yoyo-system]
  c/Lifecycle
  (start [this]
    (let [!yoyo-component (promise)
          !yoyo-system-stopped (promise)
          !yoyo-system (y/run-system! (fn [latch]
                                        (make-yoyo-system (fn [yoyo-component]
                                                            (deliver !yoyo-component yoyo-component)
                                                            (latch)
                                                            (deliver !yoyo-system-stopped nil)))))]
      (assoc this
        :yoyo-component @!yoyo-component
        :!yoyo-system !yoyo-system
        :!yoyo-system-stopped !yoyo-system-stopped)))

  (stop [{:keys [!yoyo-system !yoyo-system-stopped] :as this}]
    (deliver !yoyo-system nil)
    @!yoyo-system-stopped
    (dissoc this :db-pool :!yoyo-system)))
