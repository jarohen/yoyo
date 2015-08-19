(ns yoyo.system-test
  (:require [yoyo.system :refer :all]
            [yoyo.core :as yc]
            [cats.core :as c]
            [clojure.test :refer :all]))

(deftest run-test
  (is (= (run (c/mlet [c1 (ask :c1)
                       c2 (ask :c2)]
                (->dep {:my-c1 c1, :my-c2 c2}))

           {:c1 :the-c1
            :c2 :a-wild-c2-appeared})

         {:my-c1 :the-c1,
          :my-c2 :a-wild-c2-appeared})))

(deftest make-system-test
  (let [!events (atom [])
        add-event! (fn [event]
                     (swap! !events conj event))

        make-c1 (-> (->dep (yc/->component (do
                                             (add-event! :started-c1)
                                             :the-c1)
                                           (fn []
                                             (add-event! :stopped-c1))))

                    (named :c1))

        make-c2 (-> (c/mlet [c1 (ask :c1)]
                      (->dep (yc/->component (do
                                               (add-event! {:event :started-c2
                                                            :c1 c1})
                                               :the-c2)

                                             (fn []
                                               (add-event! :stopped-c2)))))

                    (named :c2))

        make-c3 (-> (c/mlet [c1 (ask :c1)
                             c2 (ask :c2)]
                      (->dep (yc/->component (do
                                               (add-event! {:event :started-c3
                                                            :c1 c1
                                                            :c2 c2})
                                               :the-c3))))

                    (named :c3))

        make-the-system (fn []
                          (make-system #{make-c1
                                         make-c2
                                         make-c3}))]

    (yc/with-system (make-the-system)
      (fn [system]
        (add-event! {:event :started-system
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
