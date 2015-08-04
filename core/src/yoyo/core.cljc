(ns yoyo.core
  (:require #?(:clj [clojure.tools.logging :as log])
            [cats.core :as c]
            [cats.protocols :as cp]))

(declare component-monad)

(defprotocol ISystem
  (stop! [_]))

(defrecord YoyoSystem [v stop-fn]
  ISystem
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

(alter-meta! #'map->YoyoSystem assoc :private true)
(alter-meta! #'->YoyoSystem assoc :private true)
(alter-meta! #'ISystem assoc :private true)

(defmethod print-method YoyoSystem [{:keys [v]} w]
  (.write w (format "#yoyo/system %s" (pr-str v))))

(defn ->component
  ([v]
   (->component v nil))

  ([v stop-fn]
   (->YoyoSystem v stop-fn)))

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
                     (fn []
                       (try
                         (when inner-stop-fn
                           (inner-stop-fn))

                         (finally
                           (when outer-stop-fn
                             (outer-stop-fn))))))))))

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

  (with-system (c/mlet [db-pool (open-db-pool)
                        web-server (open-server db-pool)]
                 (c/return {:db-pool db-pool
                            :web-server web-server}))
    (fn [system]
      (println "started:" (pr-str system))
      @system)))
