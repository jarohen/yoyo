(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  yoyo
  (:require [medley.core :as m]
            [clojure.tools.namespace.repl :as ctn]
            [clojure.tools.logging :as log]
            [clojure.test]))

(defn run-system
  "Runs the given system, in a new thread, passing it a promise latch.

  The function should start the system, call the latch function, and
  then stop, closing any necessary resources.

  Returns a function - call the function to stop the system."

  [f]

  (let [latch-promise (promise)
        started-promise (promise)
        latch (fn [& _]
                (log/info "Started system.")
                (deliver started-promise ::success)

                @latch-promise

                (log/info "Stopping system..."))

        !system-result (future
                         (log/info "Starting system...")

                         (try
                           (f latch)
                           (catch Throwable e
                             (deliver started-promise e))

                           (finally
                             (log/info "Stopped system."))))]

    (let [started-result @started-promise]
      (if (= ::success started-result)
        (fn []
          (deliver latch-promise nil)
          @!system-result)
        (throw started-result)))))

(defonce ^:private !system-fn (atom nil))
(defonce ^:private !latch (atom nil))

(defn reresolve [v]
  (if (var? v)
    (let [v-ns (doto (ns-name (:ns (meta v)))
                 require)]
      (ns-resolve (find-ns v-ns)
                  (:name (meta v))))

    v))

(defn set-system-fn!
  "Sets the Yo-yo system fn, to be used with `start!`, `stop!` and
  `reload!`.

  Usage: `(set-system-fn! #'make-system)`"

  [system-fn]

  (reset! !system-fn (comp reresolve system-fn)))

(defn preserving-previous-system [system-fn]
  (let [!previous-system (atom nil)]
    (fn [latch]
      (let [reresolved-system-fn (reresolve system-fn)
            system-result (reresolved-system-fn @!previous-system
                                                (comp latch
                                                      #(do (reset! !previous-system nil)
                                                           %&)))]
        (reset! !previous-system system-result)
        system-result))))

(defn start!
  "Starts the Yo-yo system, calling the function set by
  `set-system-fn!`"
  []

  (assert (nil? @!latch) "System already started!")

  (if-let [system-fn @!system-fn]
    (reset! !latch (run-system system-fn))

    (throw (ex-info "Please set a Yo-yo system-var!" {}))))

(defn stop!
  "Stops the currently running Yo-yo system, if there is one, no-op if
  no system is currently running.

  Returns the return value of the system."
  []

  (let [latch (m/deref-reset! !latch nil)]
    (when latch
      (latch))))

(defn reload!
  "Reloads a Yo-yo system by stopping and restarting it."
  ([]
   (reload! {}))

  ([{:keys [refresh? refresh-all?]
     :or {refresh? true
          refresh-all? false}}]
   (let [system-result (stop!)]

     (when refresh-all?
       (ctn/clear))

     (when refresh?
       (let [ctn-result (ctn/refresh)]
         (if-not (= :ok ctn-result)
           (throw ctn-result))))

     (start!))))

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
