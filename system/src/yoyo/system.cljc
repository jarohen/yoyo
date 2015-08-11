(ns yoyo.system
  (:require [yoyo.core :as yc]
            [cats.core :as c]
            [cats.protocols :as cp]))

(defrecord Dependency [id dependent])

(alter-meta! #'->Dependency assoc :private true)
(alter-meta! #'map->Dependency assoc :private true)

(defrecord Dependent [component using])

(alter-meta! #'->Dependent assoc :private true)
(alter-meta! #'map->Dependent assoc :private true)

(defn ->dep
  ([component]
   (->dep component {}))

  ([component {:keys [using]}]
   (map->Dependent {:component component
                    :using using})))

(defn named [dependent id]
  (map->Dependency {:id id
                    :dependent dependent}))

(defn deps [& dep-ids]
  (->> dep-ids
       (map (juxt identity vector))
       (into {})))

(comment
  (defn make-c1 []
    (-> (fn [_]
          (yc/->component :the-c1
                          (fn []
                            (println "stopping c1!"))))
        ->dep
        (named :c1)))

  (defn make-c2 []
    (-> (fn [{:keys [c1]}]
          (yc/->component :the-c2
                          (fn []
                            (println "stopping c2!"))))

        (->dep {:using (deps :c1)})
        (named :c2)))

  (defn make-c3 []
    (-> (fn [{:keys [c1 c2]}]
          (yc/->component :the-c3))

        (->dep {:using (deps :c1 :c2)})
        (named :c3))))

#_(make-system #{(make-c1)
                 (make-c2)
                 (make-c3)})

(defn make-system [dependencies]
  )
