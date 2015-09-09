(ns {{name}}.service.main
  (:require [{{name}}.service.handler :refer [make-handler]]
            [clojure.tools.logging :as log]
            [nrepl.embed :as nrepl]
            [yoyo :refer [ylet]]
            [yoyo.aleph :as aleph]))

(defn make-system [latch]
  (ylet [web-server (aleph/with-webserver {:handler (make-handler)
                                           :port 3000})]
    (latch)))

(defn -main []
  (nrepl/start-nrepl! {:port 7888})

  (yoyo/set-system-fn! '{{name}}.service.main/make-system)

  (yoyo/start!))
