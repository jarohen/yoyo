(ns yoyo.system
  (:require [yoyo.core :as yc]
            [yoyo.system.protocols :as p]
            [cats.core :as c]
            [cats.protocols :as cp])
  (:import [yoyo.system.protocols Dependent]))

(defn named [dependent id]
  (p/map->Dependency {:id id
                      :dependent dependent}))

(defn ask [& path]
  (p/map->Dependent {:dep-key (first path)
                     :f (fn [system]
                          (get-in system path))}))

(def !ask
  (p/map->Dependent {:dep-key ::!system
                     :f (comp ::!system meta)}))

(defn deps [& dep-ids]
  (->> dep-ids
       (map (juxt identity vector))
       (into {})))

(defn ->dep [v]
  (c/return p/dependent-monad v))

(defn askm [dep-map]
  (reduce (fn [m-system [dep-key path]]
            (c/bind m-system
                    (fn [system]
                      (c/fmap (apply ask path)
                              (fn [component]
                                (assoc system dep-key component))))))

          (->dep {})
          dep-map))

(defn run [dependent system]
  (loop [dependent dependent]
    (if (and dependent
             (instance? Dependent dependent))

      (let [new-system (p/satisfy dependent system)]
        (if (= new-system system)
          new-system
          (recur new-system)))

      dependent)))

(defn wrapf
  ([f]
   (->> !ask
        (c/fmap (fn [!system]
                  (wrapf f !system)))))

  ([f !system]
   (-> (fn [& args]
         (run (apply f args) @!system))

       (with-meta (meta f)))))

(defn make-system [dependencies]
  )

(comment
  (def make-c1
    (-> (->dep (yc/->component :the-c1
                               (fn []
                                 (println "stopping c1!"))))

        (named :c1)))

  (def make-c2
    (-> (c/mlet [c1 (ask :c1)]
          (->dep (yc/->component :the-c2
                                 (fn []
                                   (println "stopping c2!")))))

        (named :c2)))

  (def make-c3
    (-> (c/mlet [{:keys [c1 c2]} (askm (deps :c1 :c2))]
          (->dep (yc/->component :the-c3)))

        (named :c3)))

  (defn make-the-system []
    (make-system #{make-c1
                   make-c2
                   make-c3})))
