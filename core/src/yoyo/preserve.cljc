(ns yoyo.preserve
  (:require [yoyo.reresolve :as yr]))

(defn preserving-previous-system [system-fn]
  (let [!previous-system (atom nil)
        reresolving-system-fn (yr/with-reresolve system-fn)]

    (fn [f]
      ((reresolving-system-fn @!previous-system)

       (fn [_ stop!]
         (reset! !previous-system nil)

         (fn []
           (reset! !previous-system (stop!))))))))
