(ns yoyo.system-test
  (:require [yoyo.system :refer :all]
            [yoyo.core :as yc]
            [cats.core :as c]
            [clojure.test :refer :all]))

(prefer-method clojure.pprint/simple-dispatch yoyo.protocols.IComponent clojure.lang.IPersistentMap)

(defn make-c1 [!events]
  (-> (fn []
        (->dep (yc/->component (do
                                 (swap! !events conj :started-c1)
                                 :the-c1)
                               (fn []
                                 (swap! !events conj :stopped-c1)))))

      (named :c1)))

(defn make-c2 [!events]
  (-> (fn []
        (c/mlet [c1 (ask :c1)]
          (->dep (yc/->component (do
                                   (swap! !events conj {:event :started-c2
                                                        :c1 c1})
                                   :the-c2)

                                 (fn []
                                   (swap! !events conj :stopped-c2))))))

      (named :c2)))

(defn make-c3 [!events {:keys [to-throw]}]
  (-> (fn []
        (c/mlet [c1 (ask :c1)
                 c2 (ask :c2)
                 env (ask-env)]
          (when to-throw
            (throw to-throw))

          (->dep (yc/->component (do
                                   (swap! !events conj {:event :started-c3
                                                        :c1 c1
                                                        :c2 c2})
                                   :the-c3)))))

      (named :c3)))

(deftest make-system-test
  (let [!events (atom [])

        make-the-system (fn []
                          (make-system #{(make-c1 !events)
                                         (make-c2 !events)
                                         (make-c3 !events {:throw? false})}))]

    (yc/with-system (make-the-system)
      (fn [system]
        (swap! !events conj {:event :started-system
                             :system system})))

    (is (= @!events

           [:started-c1
            {:event :started-c2, :c1 :the-c1}
            {:event :started-c3, :c1 :the-c1, :c2 :the-c2}
            {:event :started-system,
             :system {:c1 :the-c1,
                      :c2 :the-c2,
                      :c3 :the-c3}}
            :stopped-c2
            :stopped-c1]))))

(deftest handles-exception
  (let [!events (atom [])

        to-throw (ex-info "uh oh..." {})

        make-the-system (fn []
                          (make-system #{(make-c1 !events)
                                         (make-c2 !events)
                                         (make-c3 !events {:to-throw to-throw})}))]

    (try
      (yc/with-system (make-the-system)
        (fn [system]
          (swap! !events conj {:event :started-system
                               :system system})))

      (throw (ex-info "Should have thrown!" {}))

      (catch Exception e
        (is (= e to-throw))

        (is (= @!events

               [:started-c1
                {:event :started-c2, :c1 :the-c1}
                :stopped-c2
                :stopped-c1]))))))
