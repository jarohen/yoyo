(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  yoyo.system
  (:require [yoyo.core :as yc]
            [yoyo.system.watcher :as w]
            [yoyo.system.runner :as r]
            [yoyo.system.protocols :as p]
            [cats.core :as c]))

(defn ->dep [v]
  (c/return p/dependent-monad v))

(defn named [dependent-fn id]
  (p/map->InitialDependency {:id id
                             :dependent-fn dependent-fn}))

(defn ask [p & path]
  (p/map->NestedDependent {:dep-key p
                           :f (fn [system]
                                (->dep (get-in system (cons p path))))}))

(defn ask-env []
  (p/env-dependent))

(defn- try-satisfy-dependencies [m-system]
  (reduce (fn [m-system {dependency-id :id, :as dependency}]
            (let [dependent (p/get-dependent dependency)]
              (c/bind m-system

                      (fn [{:keys [system dependencies] :as acc}]
                        (let [m-new-dependent (or (when (satisfies? p/Dependent dependent)
                                                    (p/try-satisfy dependent system))
                                                  dependent)]

                          (if (satisfies? p/Dependent m-new-dependent)
                            (yc/->component (update acc :dependencies conj (p/->Dependency dependency-id m-new-dependent)))

                            (->> m-new-dependent
                                 (c/fmap (fn [new-dependent]
                                           (w/satisfy! (get-in (meta system) [:env dependency-id]) new-dependent)
                                           (assoc-in acc [:system dependency-id] new-dependent))))))))))

          (assoc-in m-system [:v :dependencies] [])

          (:dependencies @m-system)))

(defn- cycle-error [m-system]
  (let [{:keys [dependencies system]} @m-system]
    (ex-info "Cannot satisfy any dependencies! Is there a cycle?"
             {:started (set (keys system))
              :dependencies (->> dependencies
                                 (map (juxt :id (comp :dep-key :dependent)))
                                 (into {}))})))

(defn make-system [dependencies]
  (loop [m-system (let [env (zipmap (map :id dependencies)
                                    (repeatedly w/watcher))]
                    (yc/->component (-> {:system (-> {}
                                                     (with-meta {:env env}))
                                         :dependencies dependencies})

                                    (fn []
                                      (doseq [watcher (vals env)]
                                        (w/close! watcher)))))]

    (let [dependencies (:dependencies @m-system)]
      (if (empty? dependencies)
        (c/fmap :system m-system)

        (let [new-m-system (try-satisfy-dependencies m-system)]
          (if (not= (:dependencies @new-m-system) dependencies)
            (recur new-m-system)
            (throw (cycle-error new-m-system))))))))

(defn run [dependent system]
  (r/run dependent system))


#?(:clj
   [(defn run!! [dependent env]
      (r/run!! dependent env))

    (defn wrap-run!! [f env]
      (r/wrap-run!! f env))])

(defn run-async [dependent env]
  (r/run-async dependent env))

(defn wrap-run-async [f env]
  (r/wrap-run-async f env))
