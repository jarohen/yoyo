(ns yoyo.system
  (:require [yoyo.core :as yc]
            [medley.core :as m]
            [clojure.set :as set]
            [cats.core :as c]
            [cats.protocols :as cp]))

(comment
  (defn verify-deps [system-map]
    (doseq [[component-key {:keys [deps]}] system-map
            [_ [dep]] deps]
      (when-not (contains? system-map dep)
        (throw (ex-info "Missing dependency!" {:dependency dep
                                               :dependent component-key})))))

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
                               (m/map-vals (comp set #(map first %) vals :deps))
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
                        (map #(update % :deps set/difference with-no-more-deps))))))))))

(declare dependent-monad)

(defrecord ^:private Dependency [dep-id component])

(defn ->dep [dep-id component]
  (map->Dependency {:dep-id dep-id
                    :component component}))

(alter-meta! #'->Dependency assoc :private true)
(alter-meta! #'map->Dependency assoc :private true)

(defrecord Dependent [dep-keys f]
  cp/Context
  (get-context [_]
    dependent-monad))

(alter-meta! #'->Dependent assoc :private true)
(alter-meta! #'map->Dependent assoc :private true)

(defn make-system [dependencies]
  )

(defn ask [& system-path]
  (map->Dependent {:dep-keys #{(first system-path)}
                   :f (fn [system]
                        (get-in system system-path))}))

(def dependent-monad
  (reify
    cp/Functor
    (fmap [_ f fv]
      )

    cp/Monad
    (mreturn [_ v]
      (map->Dependent {:dep-keys #{}
                       :f (fn [dep]
                            v)}))

    (mbind [_ {outer-deps :dep-keys, outer-f :f} f]
      (map->Dependent {:dep-keys outer-deps
                       :f (fn [deps]
                            (f (outer-f deps)))}))))

(defn satisfy [dependent system]
  ((:f dependent) system))

(comment
  (c/with-monad dependent-monad
    (-> (doto (c/bind (ask :config :s)
                      (fn [config]
                        (doto (c/bind (ask :config :bar)
                                      (fn [config-2]
                                        (do (c/return {:the-config config, :the-config2 config-2}))))
                          (prn :inner))))
          prn)
        (satisfy {:config {:s :foo
                           :bar :bax}})
        (satisfy {:config {:s :bar
                           :bar :quux}})
        (#((:f %) nil)))))

(map->Dependent {:dep-key :config
                 :f (fn [s]
                      (:config s))})

(comment
  (defn m-make-c1 []
    (->dep :c1
           (yc/->component :the-c1 (fn []
                                     (println "stopping c1!")))))

  (defn m-make-c2 []
    (->dep :c2
           (c/mlet [c1 (ask :c1)]
             (c/return (yc/->component :the-c2
                                       (fn []
                                         (println "stopping c2!")))))))

  (defn m-make-c3 []
    (->dep :c3
           (c/mlet [c1 (ask :c1)
                    c2 (ask :c2)]
             (c/return :the-c3))))

  (make-system #{(m-make-c1)
                 (m-make-c2)
                 (m-make-c3)}))
