(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  yoyo
  (:require [yoyo.core :as yc]
            [medley.core :as m]
            #?@(:clj
                [[clojure.tools.namespace.repl :as ctn]])))

(defonce ^:private !system-fn (atom nil))
(defonce ^:private !latch (atom nil))

(defn- reresolve [v]
  (if (var? v)
    (fn [& args]
      (apply #?(:clj
                (let [v-ns (doto (ns-name (:ns (meta v)))
                             require)]
                  (ns-resolve (find-ns v-ns)
                              (:name (meta v))))

                :cljs
                v)

             args))
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
                                                (fn [& args]
                                                  (reset! !previous-system nil)
                                                  (apply latch args)))]
        (reset! !previous-system system-result)
        system-result))))

(defn start!
  "Starts the Yo-yo system, calling the function set by
  `set-system-fn!`"
  []

  (assert (nil? @!latch) "System already started!")

  (if-let [system-fn @!system-fn]
    (reset! !latch (yc/run-system system-fn))

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

   (when refresh-all?
     #?(:clj (ctn/clear)))

   (when refresh?
     #?(:clj (let [ctn-result (ctn/refresh)]
               (if-not (= :ok ctn-result)
                 (throw ctn-result)))))

   (start!)))
