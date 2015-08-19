(ns yoyo.system.watcher
  (:require #?(:clj
               [clojure.core.async :as a]
               :cljs
               [cljs.core.async :as a])))

(defprotocol IWatcher
  (await! [_])
  (satisfy! [_ v]))

(defrecord Watcher [!chs]
  IWatcher
  (await! [_]
    (let [ch (a/chan)]
      (loop []
        (let [{:keys [chs v] :as state} @!chs]
          (if (= v ::nil)
            (when-not (compare-and-set! !chs state (update state :chs conj ch))
              (recur))

            (do
              (a/put! ch v)
              (a/close! ch)))))

      ch))

  (satisfy! [_ new-v]
    (loop []
      (let [{:keys [chs v] :as state} @!chs]
        (when (= v ::nil)
          (if (compare-and-set! !chs state (assoc state :v new-v))
            (doseq [ch chs]
              (a/put! ch new-v)
              (a/close! ch))

            (recur)))))))



(defn watcher
  ([]
   (watcher ::nil))

  ([v]
   (->Watcher (atom {:chs #{}
                     :v v}))))

(comment
  (def foo-watcher (watcher))

  (def foo-ch-1 (await! foo-watcher))
  (def foo-ch-2 (await! foo-watcher))

  (satisfy! foo-watcher :thing)


  (a/<!! foo-ch-2))
