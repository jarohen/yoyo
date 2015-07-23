(ns yoyo.system
  (:require [yoyo.core :as yc]
            [medley.core :as m]
            [schema.core :as sc]
            [com.stuartsierra.dependency :as deps]))

(defn- constant-value [v]
  (fn [app f]
    (f v)))

(defn- analyze-deps [system]
  (->> (for [[k v] system]
         [k {:component-fn (if (fn? v)
                             v
                             (constant-value v))
             :deps (::deps (meta v))}])
       (into {})))

(defn sort-deps [analyzed-deps]
  (->> (reduce (fn [graph [component-key {:keys [deps]}]]
                 (reduce (fn [graph [dep-key [dep-val & _]]]
                           (deps/depend graph component-key dep-val))
                         (-> graph
                             (deps/depend ::system component-key))
                         deps))
               (deps/graph)
               analyzed-deps)

       deps/topo-sort
       (remove #{::system})))

(defn make-system [system]
  (let [analyzed-deps (analyze-deps system)
        sorted-deps (sort-deps analyzed-deps)]

    (reduce (fn [f dep-key]
              (-> f
                  (yc/chain (fn [app]
                              (if-let [{:keys [component-fn deps]} (get analyzed-deps dep-key)]
                                (-> (component-fn (m/map-vals #(get-in app %) (or deps {})))
                                    (yc/chain (fn [component-value]
                                                (fn [f]
                                                  (f (assoc app dep-key component-value) (fn []))))))

                                (throw (ex-info "Can't find dependency:" {:dep dep-key})))))))

            (fn [f]
              (f {}
                 (fn [])))

            sorted-deps)))

(sc/defn ^:always-validate using [component, deps :- {sc/Any [sc/Any]}]
  (-> component
      (vary-meta assoc ::deps deps)))

(defn without-lifecycle [f]
  ;; TODO
  #_(with-meta (fn [app latch]
                 (latch (f app)))
      (meta f)))

(defn with-system-put-to [system sym]
  ;; TODO
  #_(fn [latch]
      (system (fn [started-system]
                (let [the-ns (doto (symbol (namespace sym))
                               create-ns)
                      the-name (symbol (name sym))]
                  (intern the-ns the-name started-system)

                  (let [result (latch started-system)]
                    (intern the-ns the-name nil)
                    result))))))


(comment
  (defn with-c1 [app]
    (fn [f]
      (f {:the-c1 :a-c1}
         (fn []
           (println "stopping c1")))))

  (defn with-c2 [{:keys [c1]}]
    (fn [f]
      (f {:my-c1 c1}
         (fn []
           (println "stopping c2")))))

  (defn with-c3 [{:keys [c1 c2]}]
    (fn [f]
      (f {:my-c2 c2
          :my-c1 c1}
         (fn []
           (println "stopping c3")))))

  (let [system-fn (-> (make-system {:c1 with-c1
                                    :c2 (-> with-c2
                                            (using {:c1 [:c1]}))
                                    :c3 (-> with-c3
                                            (using {:c2 [:c2]
                                                    :c1 [:c1 :the-c1]}))})
                      #_(with-system-put-to 'user/foo-system))

        started-system (yc/run-system (-> system-fn
                                          (yc/chain
                                           (fn [running-system]
                                             (fn [f]
                                               (prn running-system)
                                               #_(clojure.pprint/pprint running-system)
                                               #_(clojure.pprint/pprint (eval 'user/foo-system))
                                               #_(prn (= running-system (eval 'user/foo-system)))
                                               (f nil
                                                  (fn [])))))))

        _ (println "stopping system!")]

    (started-system)

    ))
