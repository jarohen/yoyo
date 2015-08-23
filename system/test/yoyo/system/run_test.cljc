(ns yoyo.system.run-test
  (:require [yoyo.system.run :refer :all]
            [yoyo.system :as ys]
            [yoyo.core :as yc]
            [cats.core :as c]
            [clojure.core.async :as a]
            [clojure.test :refer :all]
            [slingshot.slingshot :refer [try+ throw+]]))

(deftest run-test
  (is (= (run (c/mlet [c1 (ys/ask :c1)
                       c2 (ys/ask :c2)]
                (ys/->dep {:my-c1 c1, :my-c2 c2}))

           {:c1 :the-c1
            :c2 :a-wild-c2-appeared})

         {:my-c1 :the-c1,
          :my-c2 :a-wild-c2-appeared})))

(defn make-dep [{:keys [!!val wrap-fn]}]
  (-> (fn []
        (c/mlet [env (ys/ask-env)]
          (let [!val (future-call (fn []
                                    (-> (c/mlet [c1 (ys/ask :c1)]
                                          (ys/->dep {:my-c1 c1}))

                                        (wrap-fn env))))]
            (deliver !!val !val)

            (ys/->dep (yc/->component !val)))))

      (ys/named :dep)))

(defn make-c1 [{:keys [to-throw]}]
  (-> (fn []
        (c/mlet [env (ys/ask-env)]
          (ys/->dep (yc/->component (do (Thread/sleep 500)
                                        (when to-throw
                                          (throw to-throw))
                                        :the-c1)))))

      (ys/named :c1)))

(def ys-error?
  (every-pred map?
              #(contains? % ::ys/error)))

(deftest runs-sync
  (let [!!val (promise)]
    (yc/with-system (ys/make-system [(make-dep {:!!val !!val
                                                :wrap-fn run!!})
                                     (make-c1 {:to-throw nil})])
      (fn [system]
        (is (= (:c1 system)
               :the-c1))

        (is (future? (:dep system)))

        (is (= @@!!val
               {:my-c1 :the-c1}))))))

(deftest runs-sync-without-dep
  (let [!!val (promise)]
    (yc/with-system (ys/make-system [(make-dep {:!!val !!val
                                                :wrap-fn run!!})])
      (fn [system]
        (try+
          @@!!val

          (throw+ :should-have-thrown)

          (catch ys-error? {error ::ys/error}
            (is (= error :no-such-dependency))))))))

(deftest runs-sync-with-error
  (let [!!val (promise)
        to-throw (ex-info "uh oh!" {})]
    (try
      (ys/make-system [(make-dep {:!!val !!val
                                  :wrap-fn run!!})
                       (make-c1 {:to-throw to-throw})])

      (throw+ :should-have-thrown)

      (catch Exception e
        (is (= e to-throw))))

    (try+
      @@!!val

      (throw+ :should-have-thrown)

      (catch ys-error? {error ::ys/error}
        (is (= error :system-failed))))))

(deftest runs-async
  (let [!!val (promise)]
    (yc/with-system (ys/make-system [(make-dep {:!!val !!val
                                                :wrap-fn run-async})
                                     (make-c1 {:to-throw nil})])
      (fn [system]
        (is (= (:c1 system)
               :the-c1))

        (is (future? (:dep system)))

        (is (= (a/<!! @@!!val)
               {:my-c1 :the-c1}))))))

(deftest runs-async-with-error
  (let [!!val (promise)
        to-throw (ex-info "uh oh!" {})]
    (try
      (ys/make-system [(make-dep {:!!val !!val
                                  :wrap-fn run-async})
                       (make-c1 {:to-throw to-throw})])

      (catch Exception e
        (is (= e to-throw))

        (let [returned-e (a/<!! @@!!val)]
          (is (= ::ys/system-failed returned-e)))))))
