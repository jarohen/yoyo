(ns yoyo.system
  (:require [yoyo.core :as yc]
            [yoyo.system.watcher :as w]
            [yoyo.protocols :as yp]
            [yoyo.system.protocols :as p]
            [cats.core :as c]
            [cats.protocols :as cp])
  (:import [yoyo.system.protocols Dependency]))

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

(defn wrap-fn [f env]
  )

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
