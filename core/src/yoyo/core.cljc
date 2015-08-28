(ns yoyo.core
  (:require [yoyo.protocols :as p]
            [yoyo.sink :as sink]
            [cats.core :as c]))

(defn ->component
  ([v]
   (->component v nil))

  ([v stop-fn]
   (p/->YoyoComponent v stop-fn)))

(defn with-system [system f]
  (let [res (f (:v system))]
    (p/stop! system)
    res))

(defn with-system-put-to [system sink]
  (c/mlet [running-system system]
    (sink/set-system! sink running-system)
    (->component running-system
                 (fn []
                   (sink/set-system! sink nil)))))

(comment
  (defn open-db-pool []
    (println "opening db pool...")
    (->component :db-pool
                 (fn []
                   (println "closing db pool!"))))

  (defn open-server [db-pool]
    (println "got db pool:" db-pool)
    (->component :server
                 (fn []
                   (println "closing server!"))))

  (with-system (-> (c/mlet [db-pool (open-db-pool)
                            web-server (open-server db-pool)]
                     (->component {:db-pool db-pool
                                   :web-server web-server}))
                   (with-system-put-to 'user/foo-system))
    (fn [system]
      (println "started:" (pr-str system))
      @system)))
