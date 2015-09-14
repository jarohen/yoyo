(ns yoyo.system.protocols-test
  (:require [yoyo.system.protocols :refer :all]
            [cats.core :as c]
            [clojure.test :refer :all]))

(defn ->dep [v]
  (c/return dependent-monad v))

(deftest the-tests
  (is (= (let [f (:f (->NestedDependent :foo (comp ->dep :foo)))]
           (f {:foo :blah}))

         (->dep :blah)))

  (is (= (let [f (:f (->> (->NestedDependent :foo (comp ->dep :foo))
                          (c/fmap (fn [the-foo]
                                    {:the-foo the-foo}))))]
           (f {:foo :blah}))

         (->dep {:the-foo :blah})))

  (is (= (->> (->dep :foo)
              (c/fmap (fn [the-foo]
                        {:the-foo the-foo})))

         (->dep {:the-foo :foo})))

  (is (= (c/bind (->dep :foo)
                 (fn [the-foo]
                   (->dep {:the-foo the-foo})))

         (->dep {:the-foo :foo}))))
