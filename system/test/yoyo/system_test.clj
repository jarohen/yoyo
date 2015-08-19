(ns yoyo.system-test
  (:require [yoyo.system :refer :all]
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
