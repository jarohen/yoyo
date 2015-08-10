(ns yoyo.core
  (:require #?(:clj [clojure.tools.logging :as log])
            [cats.core :as c]
            [cats.protocols :as cp]
            [yoyo.sink :as sink]))

(declare component-monad)

(defprotocol IComponent
  (stop! [_]))

(defrecord YoyoComponent [v stop-fn]
  IComponent
  (stop! [_]
    (when stop-fn
      (stop-fn))
    v)

  cp/Context
  (get-context [_]
    component-monad)

  clojure.lang.IDeref
  (deref [_]
    v))

(alter-meta! #'map->YoyoComponent assoc :private true)
(alter-meta! #'->YoyoComponent assoc :private true)
(alter-meta! #'IComponent assoc :private true)

(defmethod print-method YoyoComponent [{:keys [v]} w]
  (.write w (format "#yoyo/component %s" (pr-str v))))

(defn ->component
  ([v]
   (->component v nil))

  ([v stop-fn]
   (->YoyoComponent v stop-fn)))

(defn as-component [component]
  (or (when (instance? YoyoComponent component)
        component)
      (->component component)))

(defn with-system [system f]
  (let [res (f system)]
    (stop! system)
    res))

(def component-monad
  (reify
    cp/Functor
    (fmap [_ f {outer-v :v, outer-stop-fn :stop-fn}]
      (->component (f outer-v) outer-stop-fn))

    cp/Monad
    (mreturn [_ v]
      (->component v))

    (mbind [_ {outer-v :v, outer-stop-fn :stop-fn} f]
      (let [{inner-v :v, inner-stop-fn :stop-fn} (try
                                                   (f outer-v)
                                                   (catch Throwable t
                                                     (try
                                                       (when outer-stop-fn
                                                         (outer-stop-fn))

                                                       (finally
                                                         (throw t)))))]
        (->component inner-v
                     (cond
                       (and inner-stop-fn outer-stop-fn) (fn []
                                                           (try
                                                             (when inner-stop-fn
                                                               (inner-stop-fn))

                                                             (finally
                                                               (when outer-stop-fn
                                                                 (outer-stop-fn)))))
                       outer-stop-fn outer-stop-fn
                       inner-stop-fn inner-stop-fn))))))

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
                     (c/return {:db-pool db-pool
                                :web-server web-server}))
                   (with-system-put-to 'user/foo-system))
    (fn [system]
      (println "started:" (pr-str system))
      @system)))
