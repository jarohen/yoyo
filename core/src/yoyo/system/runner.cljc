(ns yoyo.system.runner
  (:require [yoyo.system.watcher :as w]
            [yoyo.system.protocols :as p]
            [cats.core :as c]
            [clojure.walk :as w]

            #?(:clj
               [clojure.core.async :as a :refer [go go-loop]]
               :cljs
               [cljs.core.async :as a]))

  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]])))

(defn run [dependent system]
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
                                                    {:yoyo.system/error :system-failed
                                                     :dep-key dep-key})))]
          (recur (p/try-satisfy dependent
                                (-> (if dep-key
                                      (if-let [watcher (get env dep-key)]
                                        #?(:clj
                                           (let [!dep-promise (promise)
                                                 dep (w/await! watcher (fn [dep]
                                                                         (deliver !dep-promise dep)))]

                                             {dep-key (case dep
                                                        ::w/waiting (let [dep @!dep-promise]
                                                                      (case dep
                                                                        :yoyo.system/system-failed (throw-system-failed)

                                                                        dep))

                                                        :yoyo.system/system-failed (throw-system-failed)

                                                        dep)})

                                           ;; In CLJS? No blocking for you...
                                           :cljs
                                           (let [dep (w/await! watcher (fn [dep]))]
                                             (case dep
                                               ::w/waiting (throw (ex-info "Dependency not started!"
                                                                           {:dep-key dep-key}))

                                               :yoyo.system/system-failed (throw-system-failed)

                                               dep)))

                                        (throw (ex-info "Missing dependency"
                                                        {:yoyo.system/error :no-such-dependency
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
                                                                (a/put! ch (or (#{:yoyo.system/system-failed} dep)
                                                                               {dep-key dep}))))

                                            :yoyo.system/no-such-dependency)]

                                  (condp contains? dep
                                    #{:yoyo.system/system-failed :yoyo.system/no-such-dependency} (go dep)

                                    #{::w/waiting} ch

                                    (go {dep-key dep})))

                                (go {}))

                           dep (a/<! ch)]

                       (or (#{:yoyo.system/system-failed :yoyo.system/no-such-dependency} dep)

                           (a/<! (#?(:clj a/thread, :cljs go)
                                  (p/try-satisfy dependent
                                                 (-> dep
                                                     (with-meta {:env env}))))))))))

          dependent)))))

(defn <!! [dependent]
  (throw (ex-info "<!! used outside of mgo!"
                  {:dependent dependent})))

(defn <ch [dependent]
  (throw (ex-info "<ch used outside of mgo!"
                  {:dependent dependent})))

(defn ask-env []
  (p/env-dependent))

(defn var->sym [v]
  (let [{sym-name :name, sym-ns :ns} (meta v)]
    (symbol (str sym-ns) (str sym-name))))

#?(:clj
   (defn mgo [env form]
     (let [ys-env-sym (gensym "ys-env")]
       `(c/bind (ask-env)
                (fn [~ys-env-sym]
                  ~(w/postwalk (fn [o]
                               (if (symbol? o)
                                 (condp = (var->sym (resolve env o))
                                   'yoyo.system/<!! `#(run!! % ~ys-env-sym)
                                   'yoyo.system/<ch `#(run-async % ~ys-env-sym)
                                   o)
                                 o))

                             (w/macroexpand-all form)))))))

(comment
  (require '[yoyo.system :as ys]
           '[yoyo.core :as yc]
           '[cats.core :as c])

  (def foo-system
    (ys/make-system [(-> (fn []
                           (c/mlet [env (ys/ask-env)]
                             (ys/->dep
                              (yc/->component (future-call (fn []
                                                             (prn "out:"
                                                                  (a/<!! (do
                                                                           (println "here!")
                                                                           (-> (c/mlet [c1 (ys/ask :c1)]
                                                                                 (println "got it!" c1)
                                                                                 (Thread/sleep 2000)
                                                                                 (ys/->dep {:my-c1 c1}))

                                                                               (run-async env)))))))))))
                         (ys/named :dep))

                     (-> (fn []
                           (c/mlet [env (ys/ask-env)]
                             (ys/->dep (yc/->component (do (Thread/sleep 1000)
                                                           #_(throw (ex-info "uh oh" {}))
                                                           :the-c1)))))

                         (ys/named :c1))]))

  (:dep @foo-system))
