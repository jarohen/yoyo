(ns yoyo.system
  (:require [medley.core :as m]
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
  (-> (reduce (fn [graph [component-key {:keys [deps]}]]
                (reduce (fn [graph [dep-key [dep-val & _]]]
                          (deps/depend graph component-key dep-val))
                        graph
                        deps))
              (deps/graph)
              analyzed-deps)

      deps/topo-sort))

(defn make-system [system-fn]
  (fn [latch]
    (let [system (system-fn)
          analyzed-deps (analyze-deps system)

          system-to-run (reduce (fn [f dep-key]
                                  (fn [app]
                                    (if-let [{:keys [component-fn deps]} (get analyzed-deps dep-key)]
                                      (component-fn (m/map-vals #(get-in app %) (or deps {}))
                                                    (fn [component-value]
                                                      (f (assoc app dep-key component-value))))

                                      (throw (ex-info "Can't find dependency:" {:dep dep-key})))))
                                latch
                                (reverse (sort-deps analyzed-deps)))]
      (system-to-run {}))))

(sc/defn ^:always-validate using [component deps :- {sc/Any [sc/Any]}]
  (-> component
      (vary-meta assoc ::deps deps)))

(defn without-lifecycle [f]
  (with-meta (fn [app latch]
               (latch (f app)))
    (meta f)))

(defn with-system-put-to [system sym]
  (fn [latch]
    (system (fn [started-system]
              (let [the-ns (doto (symbol (namespace sym))
                             create-ns)
                    the-name (symbol (name sym))]
                (intern the-ns the-name started-system)

                (let [result (latch started-system)]
                  (intern the-ns the-name nil)
                  result))))))


(comment
  (defn with-c1 [app f]
    (f {:the-c1 :a-c1}))

  (defn with-c2 [{:keys [c1]} f]
    (f {:my-c1 c1}))

  (defn with-c3 [{:keys [c1 c2]} f]
    (f {:my-c2 c2
        :my-c1 c1}))

  (let [system-fn (-> (make-system (fn []
                                     {:c1 with-c1
                                      :c2 (-> with-c2
                                              (using {:c1 [:c1]}))
                                      :c3 (-> with-c3
                                              (using {:c2 [:c2]
                                                      :c1 [:c1 :the-c1]}))}))
                      (with-system-put-to 'user/foo-system))]
    (system-fn (fn [running-system]
                 (clojure.pprint/pprint running-system)
                 (clojure.pprint/pprint (eval 'user/foo-system))
                 (prn (= running-system (eval 'user/foo-system)))))))
