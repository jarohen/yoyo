(ns yoyo.system
  (:require [yoyo.core :as yc]
            [yoyo.system.watcher :as w]
            [yoyo.system.runner :as r]
            [yoyo.system.protocols :as p]
            [cats.core :as c]))

(defn ->dep
  " :: a -> Dependent a

  `return`, for the Dependent monad"
  [v]

  (p/->ResolvedDependent v))

(def nothing
  (->dep nil))

(defn named
  " :: (() -> Dependent a) -> Dependency a

  Given a function returning a Dependent, returns a named Dependency
  for use in `make-system`"
  [dependent-fn id]

  (p/map->InitialDependency {:id id
                             :dependent-fn dependent-fn}))

(defn ask
  " :: path -> Dependent a

  Given a path into the system map, returns a Dependent awaiting the given path.

  Essentially, behaves as if it calls `get-in` on the system with the
  provided path.

  Usage:

    (assuming a system with a dependency named `:config` - a map
     containing {:s3 {:creds ..., :bucket \"...\"}})

    (defn m-get-object [s3-path]
      (c/mlet [{:keys [creds bucket]} (ys/ask :config :s3)]
        (ys/->dep
         (aws.sdk.s3/get-object creds bucket s3-path))))"

  [p & path]

  (p/map->NestedDependent {:dep-key p
                           :f (fn [system]
                                (->dep (get-in system (cons p path))))}))

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

(defn make-system
  ":: #{Dependency} -> System"
  [dependencies]

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

            ;; We fmap s.t., if there are dep errors, we shut down the
            ;; system
            (->> m-system
                 (c/fmap (fn [_]
                           (throw (cycle-error new-m-system)))))))))))

(defn run
  " :: Dependent a -> System -> a"
  [dependent system]

  (r/run dependent system))

(defn <!! [dependent]
  (r/<!! dependent))

(defn <ch [dependent]
  (r/<ch dependent))

#?(:clj
   (defmacro mgo [& body]
     (r/mgo &env `(do ~@body))))
