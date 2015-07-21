(ns yoyo.core
  (:require #?(:clj [clojure.tools.logging :as log])))

(defn chain [component cont]
  (fn [f]
    (component (fn [outer-component outer-stop!]
                 (try
                   (let [start-inner! (cont outer-component)]
                     (start-inner! (fn [inner-component inner-stop!]
                                     (f inner-component
                                        (fn []
                                          (try
                                            (inner-stop!)
                                            (finally
                                              (outer-stop!))))))))
                   (catch Exception e
                     (outer-stop!)
                     (throw e)))))))

(defn result [v]
  (fn [f]
    (f v (constantly v))))

(defn run-system [system]

  (letfn [(log [msg]
            (#?(:clj
                log/info

                :cljs
                js/console.info)

               msg))]

    (log "Starting system...")

    (system (fn [c stop!]
              (log "Started system.")

              (fn []
                (log "Stopping system...")

                (let [result (stop!)]
                  (log "Stopped system.")
                  result))))))

#?(:clj (defmacro ylet
          "Macro to simplify 'function staircases', similar to Clojure's let.

  Every right-hand-side expression is expected to be a component - a
  function expecting the continuation parameter.

  Similarly to 'for', you can pass `:let [...]` to break out of the
  special ylet binding behaviour, and revert to a normal set of 'let'
  bindings.

  Example:

  (defn with-db-pool [opts f]
    ...
    (f pool)
    ...)

  (defn with-web-server [handler opts f]
    ...
    (f server)
    ...)

  (ylet [{:keys [...} :as db-pool} (with-db-pool db-opts)
         :let [server-opts (read-config ...)]
         web-server (with-web-server (make-handler {:db-pool db-pool})
                                     server-opts)]
    ...)

  ;; gets translated to

  (-> (with-db-pool db-opts)
      (chain (fn [{:keys [...] :as db-pool}]
               (let [server-opts (read-config ...)]
                 (-> (with-web-server (make-handler {:db-pool db-pool}
                                                    server-opts))
                     (chain (fn [web-server]
                              ...)))))))"
          [bindings & body]


          (if-let [[bind expr & more] (seq bindings)]
            (if (= bind :let)
              `(let ~expr
                 (ylet ~more ~@body))

              `(-> ~expr
                   (chain (fn [~bind]
                            (ylet ~more ~@body)))))

            `(result (do ~@body)))))
