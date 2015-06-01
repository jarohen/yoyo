(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  yoyo
  (:require [medley.core :as m]
            [clojure.tools.namespace.repl :as tn]
            [clojure.tools.logging :as log]))

(defonce ^:private !system-fn (atom nil))
(defonce ^:private !latch (atom nil))

(defn set-system-fn! [system-fn]
  (reset! !system-fn system-fn))

(defn run-system! [f]
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

(defn stop! []
  (let [latch (m/deref-reset! !latch nil)]
    (when latch
      (deliver latch :stop!))

    (boolean latch)))

(defn reload!
  ([]
   (reload! {}))

  ([{:keys [refresh-all?] :as opts}]
   (stop!)
   (start! opts)))

(defmacro ylet [bindings & body]
  (assert (even? (count bindings)) "'ylet' must have an even number of bindings")

  (if-let [[bind expr & more] (seq bindings)]
    (if (= bind :let)
      `(let ~expr
         (ylet ~more ~@body))

      `(~@expr (fn [~bind]
                 (ylet ~more ~@body))))
    `(do
       ~@body)))

(defmacro defsystem [name bindings & body]
  `(defn ~name [latch#]
     (ylet ~bindings
       ~@body)))

(comment

  ;; Option 1 - the 'fn staircase' (current)

  (defn make-system [latch]
    (with-db-pool {...}
      (fn [db-pool]
        (let [web-config (read-config ...)]
          (with-cljs-compiler {:config web-config
                               ...}
            (fn [cljs-compiler]
              (with-web-server {:handler (make-handler {:cljs-compiler cljs-compiler
                                                        :db-pool db-pool
                                                        :config web-config})
                                :port ...}
                (fn [server]
                  (latch)))))))))

  ;; ---

  ;; Option 2 - a 'let' macro (essentially Haskell's 'do' syntax sugar
  ;; over a continuation-like monad - assuming the last argument to a
  ;; 'with-' function is the continuation)

  (:require [yoyo :refer [ylet]])

  (defn make-system [latch]
    (ylet [db-pool (with-db-pool {...})

           :let [web-config (read-config ...)]

           cljs-compiler (with-cljs-compiler {:config web-config
                                              ...})

           server (with-web-server {:handler (make-handler {:cljs-compiler cljs-compiler
                                                            :db-pool db-pool
                                                            :config web-config})
                                    :port ...})]
      (latch)))

  ;; ---

  ;; Option 3 - going slightly further - removing the latch and naming the system

  (:require [yoyo :refer [defsystem]])

  (defsystem my-system
    [db-pool (with-db-pool {...})

     :let [web-config (read-config ...)]

     cljs-compiler (with-cljs-compiler {:config web-config
                                        ...})

     server (with-web-server {:handler (make-handler {:cljs-compiler cljs-compiler
                                                      :db-pool db-pool
                                                      :config web-config})
                              :port ...})])

  ;; ---

  ;; We can, of course, offer all three!

  )
