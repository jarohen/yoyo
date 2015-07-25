(ns yoyo.system
  (:require [yoyo.core :as yc]
            [medley.core :as m]
            [clojure.set :as set]
            [schema.core :as sc]))

(defn find-cycle [deps-map]
  (loop [[todo & more] (map vector (keys deps-map))]
    (when todo
      (let [more-work (for [dep (get deps-map (last todo))]
                        (conj todo dep))]
        (if-let [the-cycle (->> more-work
                                (filter #(= (first %) (last %)))
                                first)]
          the-cycle
          (recur (concat more more-work)))))))

(defn sort-deps [system]
  (loop [topo-sorted-deps []
         remaining-deps (->> system
                             (m/map-vals (comp set first vals :deps))
                             (map #(zipmap [:dep-key :deps] %)))]
    (if (empty? remaining-deps)
      (vec topo-sorted-deps)

      (let [with-no-more-deps (->> remaining-deps
                                   (filter (comp empty? :deps))
                                   (map :dep-key)
                                   set)]
        (if (empty? with-no-more-deps)
          (throw (ex-info "Dependency cycle detected!"
                          {:cycle (find-cycle (->> remaining-deps
                                                   (map (juxt :dep-key :deps))
                                                   (into {})))}))

          (recur (concat topo-sorted-deps with-no-more-deps)
                 (->> remaining-deps
                      (remove (comp with-no-more-deps :dep-key))
                      (map #(update % :deps set/difference with-no-more-deps)))))))))

(defn make-system [system-map]
  (let [system (->> system-map
                    (m/map-vals (fn [v]
                                  {:component-fn v
                                   :deps (::deps (meta v))})))
        sorted-deps (sort-deps system)]

    (reduce (fn [f dep-key]
              (if-let [{:keys [component-fn deps]} (get system dep-key)]
                (-> f
                    (yc/chain (fn [app]
                                (-> (component-fn (m/map-vals #(get-in app %) (or deps {})))
                                    (yc/chain (fn [component-value]
                                                (fn [f]
                                                  (f (assoc app dep-key component-value) (fn [])))))))))

                (throw (ex-info "Can't find dependency:" {:dep dep-key}))))

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
