(ns yoyo.component-test
  (:require [yoyo.component :refer :all]
            [com.stuartsierra.component :as c]
            [clojure.test :refer :all]))

(deftest yoyo-in-component-test
  (let [!events (atom [])

        component (->YoyoComponent (fn [latch]
                                     (swap! !events conj :component-started)
                                     (latch :hi!)
                                     (swap! !events conj :component-stopped)))

        _ (swap! !events conj :component-created)

        started-component (c/start component)

        _ (swap! !events conj :system-running)

        stopped-component (c/stop started-component)

        _ (swap! !events conj :system-stopped)]

    (is (= @!events
           [:component-created :component-started :system-running :component-stopped :system-stopped]))))
