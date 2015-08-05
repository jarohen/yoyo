(ns yoyo.system
  (:require [yoyo.system.sink :refer [set-system!]]
            [yoyo.core :as yc]
            [medley.core :as m]
            [clojure.set :as set]))

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

(defn make-system [system-map]
  )

(defn with-system-put-to [system system-sink]
  )

(defn ask [& system-path]
    )

(defn ->dep [key component]
  )

(defn !ask []
  )

(comment
  (require '[cats.core :as c]
           '[cats.protocols :as cp])

  (defn make-c1 []
    (->dep :c1
           (yc/->component :the-c1 (fn []
                                     (println "stopping c1!")))))

  (defn m-make-c2 []
    (c/mlet [c1 (ask :c1)]
      (->dep :c2
             (yc/->component :the-c2
                             (fn []
                               (println "stopping c2!"))))))

  (defn m-make-c3 []
    (c/mlet [c1 (ask :c1)
             c2 (ask :c2)]
      (->dep :c3 :the-c3)))

  (make-system #{(make-c1)
                 (m-make-c2)
                 (m-make-c3)}))
