(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  yoyo.core
  (:require [yoyo.protocols :as p]
            [yoyo.sink :as sink]
            [cats.core :as c]))

(defn ->component
  " :: a -> Component a

  Like `return`. Takes the value of the component and, optionally, a
  function to stop the component.

  See https://github.com/jarohen/yoyo/doc/component-monad.org for more
  details"

  ([v]
   (->component v nil))

  ([v stop-fn]
   (p/->YoyoComponent v stop-fn)))

(defn with-component
  "Given a started component (simple or composite), calls the provided
  1-arg function with the component's value and, when the function's
  done, stops the component."
  [component f]

  (let [res (f @component)]
    (p/stop! component)
    res))

(defn with-system-put-to
  "Given a running system, stores the value of the system in the given
  sink (either an atom or (CLJ only) a symbol, like
  'user/foo-system).

  When the system stops, it will clear the sink."
  [system sink]

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

  (with-component (-> (c/mlet [db-pool (open-db-pool)
                               web-server (open-server db-pool)]
                        (->component {:db-pool db-pool
                                      :web-server web-server}))
                      (with-system-put-to 'user/foo-system))
    (fn [system]
      (println "started:" (pr-str system)))))
