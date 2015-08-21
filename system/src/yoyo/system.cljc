(ns yoyo.system
  (:require [yoyo.core :as yc]
            [yoyo.system.watcher :as w]
            [yoyo.protocols :as yp]
            [yoyo.system.protocols :as p]
            [cats.core :as c]
            [cats.protocols :as cp]

            #?(:clj
               [clojure.core.async :as a :refer [go-loop]]
               :cljs
               [cljs.core.async :as a]))

  (:import [yoyo.system.protocols Dependency])

  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go-loop]])))

(defn ->dep [v]
  (c/return p/dependent-monad v))

(defn named [dependent id]
  (p/map->Dependency {:id id
                      :dependent dependent}))

(defn ask [p & path]
  (p/map->NestedDependent {:dep-key p
                           :f (fn [system]
                                (->dep (get-in system (cons p path))))}))

(defn ask-env []
  (p/env-dependent))

(defn- assert-dependencies [dependencies]
  (let [non-deps (remove #(instance? Dependency %) dependencies)]
    (when-not (empty? non-deps)
      (throw (ex-info "Some of the provided dependencies aren't ... dependencies ..."
                      {:offenders non-deps})))))

(defn- try-satisfy-dependencies [{:keys [dependencies m-system]}]
  (->> dependencies
       (map #(update % :dependent p/try-satisfy (:v m-system)))
       set))

(defn- cycle-error [{:keys [dependencies m-system]}]
  (ex-info "Cannot satisfy any dependencies! Is there a cycle?"
           {:started (set (keys (:v m-system)))
            :dependencies (->> dependencies
                               (map (juxt :id (comp :dep-key :dependent)))
                               (into {}))}))

(defn- bind-system [m-system satisfied-dependencies]
  (reduce (fn [m-system {:keys [id dependent]}]
            (when-not (satisfies? yp/IComponent dependent)
              (throw (ex-info "Expecting a component"
                              {:id id
                               :got dependent})))

            (c/bind m-system
                    (fn [system]
                      (c/fmap (fn [started-component]
                                (w/satisfy! (get-in (meta system) [:env id]) started-component)
                                (assoc system id started-component))

                              dependent))))
          m-system
          satisfied-dependencies))

(defn make-system [dependencies]
  (assert-dependencies dependencies)

  (loop [{:keys [dependencies m-system] :as acc} {:dependencies dependencies
                                                  :m-system (yc/->component (-> {}
                                                                                (with-meta {:env (->> (for [{:keys [id]} dependencies]
                                                                                                        [id (w/watcher)])
                                                                                                      (into {}))})))}]

    (if (empty? dependencies)
      m-system

      (let [new-deps (try-satisfy-dependencies acc)]

        (if (= new-deps dependencies)
          (throw (cycle-error acc))

          (let [{satisfied false, unsatisfied true} (group-by (comp #(satisfies? p/Dependent %)
                                                                    :dependent)
                                                              new-deps)]

            (recur {:dependencies (set unsatisfied)
                    :m-system (bind-system m-system satisfied)})))))))

(defn run [dependent system]
  (let [system (-> system
                   (assoc ::env (->> (for [[k v] system]
                                       [k (w/watcher v)])
                                     (into {}))))]

    (loop [dependent dependent]
      (if (satisfies? p/Dependent dependent)
        (let [satisfied-dependent (p/try-satisfy dependent system)]
          (if (= satisfied-dependent dependent)
            (throw (ex-info "Can't satisfy dependent..."
                            {:dependent dependent
                             :system system
                             :missing (:dep-key dependent)}))

            (recur satisfied-dependent)))

        dependent))))

(defn- run-env* [{:keys [dep-key] :as dependent} env f]
  (letfn [(run-env** [dependent]
            (if (satisfies? p/Dependent dependent)
              (if dep-key
                (let [dep (w/await! (get env dep-key) (fn [dep]
                                                        (run-env** (p/try-satisfy dependent
                                                                                  (-> {dep-key dep}
                                                                                      (with-meta {:env env}))))))]

                  (when-not (= dep ::w/waiting)
                    (recur (p/try-satisfy dependent (-> {dep-key dep} (with-meta {:env env}))))))

                (recur (p/try-satisfy dependent (-> {} (with-meta {:env env})))))

              (f dependent)))]

    (run-env** dependent)))

(defn run-env-async [dependent env]
  (let [ch (a/chan)]
    (run-env* dependent env (fn [v]
                              (a/put! ch v)
                              (a/close! ch)))
    ch))

(defn wrap-run-env-async [f env]
  (comp #(run-env-async % env) f))

#?(:clj
   [(defn run-env [dependent env]
      (let [!v (promise)]
        (run-env* dependent env (fn [v]
                                  (deliver !v v)))
        @!v))

    (defn wrap-run-env [f env]
      (comp #(run-env % env) f))])

(comment
  (def foo-system
    (make-system [(-> (c/mlet [env (ask-env)]
                        (->dep (yc/->component (future-call (fn []
                                                              (println "here!")
                                                              (-> (c/mlet [c1 (ask :c1)]
                                                                    (println "got it!")
                                                                    (Thread/sleep 2000)
                                                                    (->dep {:my-c1 c1}))

                                                                  (run-env env)))))))
                      (named :dep))

                  (-> (c/mlet [env (ask-env)]
                        (->dep (yc/->component (do (Thread/sleep 2000) :the-c1))))
                      (named :c1))]))

  (:dep (:v foo-system)))
