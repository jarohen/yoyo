(ns yoyo.dep.run
  (:require [yoyo.dep.protocols :as p]
            [yoyo.dep.run :as r]
            [yoyo.dep.watcher :as w]

            #?(:clj
               [clojure.core.async :as a :refer [go go-loop]]
               :cljs
               [cljs.core.async :as a]))

  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]])))

(defn run
  ":: Dependent a -> System -> a"
  [dependent system]

  (let [system (-> system
                   (with-meta {:env (->> (for [[k v] system]
                                           [k (w/watcher v)])
                                         (into {}))}))]

    (loop [dependent dependent]
      (if (p/dependent? dependent)
        (let [satisfied-dependent (p/try-satisfy dependent system)]
          (if (= satisfied-dependent dependent)
            (throw (ex-info "Can't satisfy dependent..."
                            {:dependent dependent
                             :system system
                             :missing (:dep-key dependent)}))

            (recur satisfied-dependent)))

        dependent))))

(defn run!! [dependent env]
  (if (fn? dependent)
    (comp #(run!! % env) dependent)

    (loop [dependent dependent]
      (if (p/dependent? dependent)
        (let [{:keys [dep-key]} dependent
              throw-system-failed (fn []
                                    (throw (ex-info "The system failed to start"
                                                    {:error :system-failed
                                                     :dep-key dep-key})))]
          (recur (p/try-satisfy dependent
                                (-> (if dep-key
                                      (if-let [watcher (get env dep-key)]
                                        #?(:clj
                                           (let [!dep-promise (promise)
                                                 dep (w/await! watcher (fn [dep]
                                                                         (deliver !dep-promise dep)))]

                                             {dep-key (case dep
                                                        :waiting (let [dep @!dep-promise]
                                                                   (case dep
                                                                     :system-failed (throw-system-failed)

                                                                     dep))

                                                        :system-failed (throw-system-failed)

                                                        dep)})

                                           ;; In CLJS? No blocking for you...
                                           :cljs
                                           (let [dep (w/await! watcher (fn [dep]))]
                                             (case dep
                                               :waiting (throw (ex-info "Dependency not started!"
                                                                        {:dep-key dep-key}))

                                               :system-failed (throw-system-failed)

                                               dep)))

                                        (throw (ex-info "Missing dependency"
                                                        {:error :no-such-dependency
                                                         :dep-key dep-key})))
                                      {})
                                    (with-meta {:env env})))))

        dependent))))

(defn run-async [dependent env]
  (if (fn? dependent)
    (comp #(run-async % env) dependent)

    (go-loop [dependent-ch (go dependent)]
      (let [dependent (a/<! dependent-ch)]
        (if (p/dependent? dependent)
          (let [{:keys [dep-key]} dependent]
            (recur (go
                     (let [ch (if dep-key
                                (let [ch (a/chan)
                                      dep (if-let [watcher (get env dep-key)]
                                            (w/await! watcher (fn [dep]
                                                                (a/put! ch (or (#{:system-failed} dep)
                                                                               {dep-key dep}))))

                                            :no-such-dependency)]

                                  (condp contains? dep
                                    #{:system-failed :no-such-dependency} (go dep)

                                    #{:waiting} ch

                                    (go {dep-key dep})))

                                (go {}))

                           dep (a/<! ch)]

                       (or (#{:system-failed :no-such-dependency} dep)

                           (a/<! (#?(:clj a/thread, :cljs go)
                                  (p/try-satisfy dependent
                                                 (-> dep
                                                     (with-meta {:env env}))))))))))

          dependent)))))
