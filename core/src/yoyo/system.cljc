(ns yoyo.system
  (:require [yoyo.resource :as yr]
            [yoyo.dep.watcher :as w]
            [yoyo.system.protocols :as p]
            [yoyo.dep.protocols :as ydp]
            [cats.core :as c]))

(defn named
  " :: (() -> Dependent a) -> Dependency a

  Given a function returning a Dependent, returns a named Dependency
  for use in `make-system`"
  [dependent-fn id]

  (p/map->InitialDependency {:id id
                             :dependent-fn dependent-fn}))

(defn- try-satisfy-dependencies [m-system]
  (reduce (fn [m-system {dependency-id :id, :as dependency}]
            (let [dependent (p/get-dependent dependency)]
              (c/bind m-system

                      (fn [{:keys [system dependencies] :as acc}]
                        (let [m-new-dependent (or (when (ydp/dependent? dependent)
                                                    (ydp/try-satisfy dependent system))
                                                  dependent)]

                          (if (ydp/dependent? m-new-dependent)
                            (yr/->resource (update acc :dependencies conj (p/->Dependency dependency-id m-new-dependent)))

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

(defn make-system
  ":: #{Dependency} -> System"
  [dependencies]

  (loop [m-system (let [env (zipmap (map :id dependencies)
                                    (repeatedly w/watcher))]
                    (yr/->resource (-> {:system (-> {}
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

            ;; We fmap s.t., if there are dep errors, we shut down the
            ;; system
            (->> m-system
                 (c/fmap (fn [_]
                           (throw (cycle-error new-m-system)))))))))))
