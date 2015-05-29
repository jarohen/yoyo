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
        latch (fn []
                (log/info "Started system.")
                (deref latch-promise)
                (log/info "Stopping system..."))]
    (future
      (log/info "Starting system...")
      (f latch)
      (log/info "Stopped system."))

    latch-promise))

(defn- do-start! []
  (if-let [system-fn @!system-fn]
    (do
      (require (symbol (namespace system-fn)))
      (reset! !latch (run-system! (resolve system-fn)))
      true)

    (throw (ex-info "Please set a Yoyo system-fn!" {}))))

(defn start! []
  (if-not @!latch
    (tn/refresh :after 'yoyo/do-start!)

    (throw (ex-info "System already started!" {}))))

(defn stop! []
  (let [latch (m/deref-reset! !latch nil)]
    (when latch
      (deliver latch :stop!))

    (boolean latch)))

(defn reload! []
  (stop!)
  (start!))
