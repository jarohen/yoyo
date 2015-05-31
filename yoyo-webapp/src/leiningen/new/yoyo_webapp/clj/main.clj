(ns {{name}}.service.main
  (:require [{{name}}.service.handler :refer [make-handler]]
            [{{name}}.service.cljs :as cljs]
            [clojure.tools.logging :as log]
            [nrepl.embed :as nrepl]
            yoyo
            [yoyo.aleph :as aleph]))

(defn make-system [latch]
  (cljs/with-cljs-compiler
    (fn [cljs-compiler]
      (aleph/with-webserver {:handler (make-handler {:cljs-compiler cljs-compiler})
                             :port 3000}
        (fn [_]
          (latch))))))

(defn build! []
  (cljs/build-cljs!)
  (System/exit 0))

(defn -main []
  (nrepl/start-nrepl! {:port 7888})

  (yoyo/set-system-fn! '{{name}}.service.main/make-system)

  (yoyo/start!))
