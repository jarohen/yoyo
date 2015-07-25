(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  yoyo
  (:require [yoyo.core :as yc]
            [yoyo.reresolve :as yr]
            [medley.core :as m]
            #?@(:clj
                [[clojure.tools.namespace.repl :as ctn]])))

(defonce ^:private !system-fn (atom nil))
(defonce ^:private !latch (atom nil))

(defn set-system-fn!
  "Sets the Yo-yo system fn, to be used with `start!`, `stop!` and
  `reload!`.

  Can be passed a function, a var, or a fully-qualified symbol.

  Usage: `(set-system-fn! 'myapp.main/make-system)`
         `(set-system-fn! #'make-system)`
         `(set-system-fn! make-system)`"

  [system-fn]

  (reset! !system-fn (yr/with-reresolve system-fn)))

(defn start!
  "Starts the Yo-yo system, calling the function set by
  `set-system-fn!`"
  []

  (assert (nil? @!latch) "System already started!")

  (if-let [system-fn @!system-fn]
    (boolean (reset! !latch (yc/run-system system-fn)))

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

   (stop!)

   #?@(:clj
       [(when refresh-all?
          (ctn/clear))

        (when refresh?
          (let [ctn-result (ctn/refresh)]
            (when-not (= :ok ctn-result)
              (throw ctn-result))))])

   (start!)))
