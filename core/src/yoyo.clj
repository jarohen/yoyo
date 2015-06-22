(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  yoyo
  (:require [medley.core :as m]
            [clojure.tools.namespace.repl :as tn]
            [clojure.tools.logging :as log]))

(defonce ^:private !system-fn (atom nil))
(defonce ^:private !latch (atom nil))

(defn set-system-fn!
  "Sets the Yo-yo system function (a namespace-qualified symbol), to be used with `start!`, `stop!`
  and `reload!`.

  Usage: `(set-system-fn! 'myapp.main/make-system)`"

  [system-fn]

  (reset! !system-fn system-fn))

(defn run-system!
  "Runs the given system, in a new thread, passing it a promise latch.

  The function should start the system, call the latch function, and
  then stop, closing any necessary resources.

  Returns a promise - deliver any value to this promise to stop the
  system."

  [f]

  (let [latch-promise (promise)
        started-promise (promise)
        latch (fn []
                (log/info "Started system.")
                (deliver started-promise ::success)

                (deref latch-promise)

                (log/info "Stopping system..."))]
    (future
      (log/info "Starting system...")

      (try
        (f latch)
        (catch Throwable e
          (deliver started-promise e))

        (finally
          (log/info "Stopped system."))))

    (let [started-result @started-promise]
      (if (= ::success started-result)
        latch-promise
        (throw started-result)))))

(defn- do-start! []
  (if-let [system-fn @!system-fn]
    (do
      (require (symbol (namespace system-fn)))
      (reset! !latch (run-system! (resolve system-fn)))
      true)

    (throw (ex-info "Please set a Yo-yo system-fn!" {}))))

(defn start!
  "Reloads any changed namespaces, then starts the Yo-yo system.

  If `refresh-all?` is passed, reloads all namespaces.

  Usage: `(start!)` or `(start! {:refresh-all? true})`"

  ([]
   (start! {:refresh-all? false}))

  ([{:keys [refresh-all?]}]
   (if-not @!latch
     (binding [*ns* *ns*  ; *ns* seems to have to be thread-bound for refresh to work
               clojure.test/*load-tests* false]
       (if refresh-all?
         (tn/refresh-all :after 'yoyo/do-start!)
         (tn/refresh :after 'yoyo/do-start!)))

     (throw (ex-info "System already started!" {})))))

(defn stop!
  "Stops the currently running Yo-yo system, if there is one, no-op if
  no system is currently running."
  []
  (let [latch (m/deref-reset! !latch nil)]
    (when latch
      (deliver latch :stop!))

    (boolean latch)))

(defn reload!
  "Reloads a Yo-yo system by stopping and restarting it.

  Accepts the same arguments as `start!`"
  ([]
   (reload! {}))

  ([{:keys [refresh-all?] :as opts}]
   (stop!)
   (start! opts)))

(defmacro ylet
  "Macro to simplify 'function staircases', similar to Clojure's let.

  Every right-hand-side expression is expected to be short by one
  parameter - `ylet` passes a continuation function, expecting one
  argument (the left-hand-size binding) to the expression as its final
  parameter.

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

  (with-db-pool db-opts
    (fn [{:keys [...] :as db-pool}]
      (let [server-opts (read-config ...)]
        (with-web-server (make-handler {:db-pool db-pool}
                                       server-opts)
          (fn [web-server]
            ...)))))
    "

  [bindings & body]

  (assert (even? (count bindings)) "'ylet' must have an even number of bindings")

  (if-let [[bind expr & more] (seq bindings)]
    (if (= bind :let)
      `(let ~expr
         (ylet ~more ~@body))

      `(~@expr (fn [~bind]
                 (ylet ~more ~@body))))
    `(do
       ~@body)))
