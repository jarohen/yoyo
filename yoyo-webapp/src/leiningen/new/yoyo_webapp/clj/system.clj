(ns {{name}}.service.system
  (:require [{{name}}.service.handler :refer [make-handler]]
            [clojure.tools.logging :as log]
            [yoyo.aleph :as aleph]
            [yoyo.cljs :as cljs]))

(def cljs-config
  {:source-path "ui-src"

   :main '{{name}}.ui.app

   :web-context-path "/js"

   :output-dir "target/cljs/"

   :dev {:optimizations :none
         :pretty-print? true}

   :build {:optimizations :advanced
           :pretty-print? false
           :classpath-prefix "js"}})

(defn make-system [latch]
  (cljs/with-cljs-compiler cljs-config
    (fn [cljs-compiler]
      (aleph/with-webserver {:handler (make-handler {:cljs-compiler cljs-compiler})
                             :port 3000}
        (fn [_]
          (latch))))))

(defn build! []
  (cljs/build-cljs! cljs-config)
  (System/exit 0))
