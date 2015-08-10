(ns yoyo.system
  (:require [yoyo.core :as yc]
            [cats.core :as c]
            [cats.protocols :as cp]
            [clojure.set :as set]))

(declare dependent-monad)

(defrecord NamedDependency [dep-id dependent])

(alter-meta! #'->NamedDependency assoc :private true)
(alter-meta! #'map->NamedDependency assoc :private true)

(defrecord Dependent [dep-key f]
  cp/Context
  (get-context [_]
    dependent-monad))

(alter-meta! #'->Dependent assoc :private true)
(alter-meta! #'map->Dependent assoc :private true)

(defn ->dep [dep-id dependent]
  (map->NamedDependency {:dep-id dep-id
                         :dependent (or (when (instance? Dependent dependent)
                                          dependent)

                                        (cp/mreturn dependent-monad dependent))}))

(defn ask [& system-path]
  (map->Dependent {:dep-key (first system-path)
                   :f (fn [system]
                        (get-in system system-path))}))

(def dependent-monad
  (reify
    cp/Monad
    (mreturn [_ v]
      (map->Dependent {:dep-key nil
                       :f (fn [dep]
                            v)}))

    (mbind [_ {outer-dep :dep-key, outer-f :f} f]
      (map->Dependent {:dep-key outer-dep
                       :f (fn [outer-dep]
                            (f (outer-f outer-dep)))}))))

(defn satisfy [dependent system]
  (c/with-monad dependent-monad
    ((:f dependent) system)))

(comment
  (defn make-c1 []
    (->dep :c1
           (yc/->component :the-c1
                           (fn []
                             (println "stopping c1!")))))

  (defn make-c2 []
    (->dep :c2
           (c/mlet [c1 (ask :c1)]
             (c/return (yc/->component :the-c2
                                       (fn []
                                         (println "stopping c2!")))))))

  (defn make-c3 []
    (->dep :c3
           (c/mlet [c1 (ask :c1)
                    c2 (ask :c2)]
             (c/return (yc/->component :the-c3))))))

#_(make-system #{(make-c1)
                 (make-c2)
                 (make-c3)})


(defn as-dependent [dependent]
  (or (when (instance? Dependent dependent)
        dependent)

      (map->Dependent {:dep-key nil
                       :f dependent})))

(defn make-system [dependencies]
  )

(defn advance-dependencies [{:keys [components dependencies]}]
  (let [{:keys [updated?] :as advanced-system} (reduce (fn [{:keys [components dependencies updated?] :as acc}
                                                            {:keys [dep-id], {:keys [dep-key dependent]} :dependency, :as dependency}]
                                                         (if (or (nil? dep-key)
                                                                 (contains? components dep-key))

                                                           (-> acc
                                                               (update :dependencies conj (satisfy dependent components))
                                                               (assoc :updated? true))

                                                           (-> acc
                                                               (update :dependencies conj dependency))))

                                                       {:components components
                                                        :dependencies []
                                                        :updated? false}

                                                       dependencies)]

    (when-not updated?
      (throw (ex-info "Cannot satisfy any more dependencies - is there a cycle?")
             {:started-components (set (keys components))
              :dependencies (->> dependencies
                                 (map (juxt :dep-id (comp :dep-key :dependency)))
                                 (into {}))}))

    (-> advanced-system
        (dissoc :updated?))))

(defn advance-system [m-system]
  (reduce (fn [m-system {:keys [dep-id dependent] :as dependency}]
            (if-not (instance? Dependent dependent)
              (c/bind m-system
                      (fn [system]
                        (c/fmap (fn [started-component]
                                  (assoc-in system [:components dep-id] started-component))
                                (yc/as-component dependent))))

              (c/fmap m-system
                      (fn [system]
                        (-> system
                            (update :dependencies conj dependency))))))

          m-system

          (get-in m-system [:v :dependencies])))

(let [dependencies #{(make-c1)
                     (make-c2)
                     (make-c3)}]
  (loop [m-system (cp/mreturn yc/component-monad
                              {:dependencies (->> dependencies
                                                  (filter (every-pred (complement nil?)
                                                                      #(instance? NamedDependency %)))
                                                  (map #(update % :dependent as-dependent)))
                               :components {}})]

    (if (empty? (get-in m-system [:v :dependencies]))
      (c/fmap :components m-system)
      (recur (-> m-system
                 (c/fmap advance-dependencies)
                 advance-system)))))
