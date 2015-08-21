(ns yoyo.system.watcher)

(defprotocol IWatcher
  (await! [_ f])
  (satisfy! [_ v]))

(defrecord Watcher [!state]
  IWatcher
  (await! [_ f]
    (loop []
      (let [{:keys [fs v] :as state} @!state]
        (if (= v ::nil)
          (if (compare-and-set! !state state (update state :fs conj f))
            ::waiting
            (recur))

          v))))

  (satisfy! [_ new-v]
    (loop []
      (let [{:keys [fs v] :as state} @!state]
        (when (= v ::nil)
          (if (compare-and-set! !state state (assoc state :v new-v))
            (doseq [f fs]
              (f new-v))

            (recur)))))
    new-v))

(defn watcher
  ([]
   (watcher ::nil))

  ([v]
   (->Watcher (atom {:fs #{}
                     :v v}))))

(comment
  (def foo-watcher (watcher))

  (await! foo-watcher #(println "I got it:" %))
  (await! foo-watcher #(println "I got it too:" %))

  (satisfy! foo-watcher :thing))
