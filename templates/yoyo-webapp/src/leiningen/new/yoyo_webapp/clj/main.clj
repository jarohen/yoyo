(ns {{name}}.service.main
  (:require [{{name}}.service.handler :refer [make-handler]]
            [{{name}}.service.cljs :as cljs]
            [clojure.tools.logging :as log]
            [nrepl.embed :as nrepl]
            [yoyo :refer [ylet]]
            [yoyo.aleph :as aleph]))

(defn make-system [latch]
  (ylet [cljs-compiler (cljs/with-cljs-compiler)
         web-server (aleph/with-webserver {:handler (make-handler {:cljs-compiler cljs-compiler})
                                           :port 3000})]
    (latch)))

(defn build! []
  (cljs/build-cljs!)
  (System/exit 0))

(defn -main []
  (nrepl/start-nrepl! {:port 7888})

  (yoyo/set-system-fn! '{{name}}.service.main/make-system)

  (yoyo/start!))
