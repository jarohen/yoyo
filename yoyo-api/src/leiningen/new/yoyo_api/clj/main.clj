(ns {{name}}.service.main
  (:require [{{name}}.service.handler :refer [make-handler]]
            [clojure.java.io :as io]
            [nomad :refer [read-config]]
            [nrepl.embed :as nrepl]
            [yoyo.aleph :as aleph]
            [yoyo :as y]
            [yoyo.system :as ys]))

(def make-system
  (-> (ys/make-system (fn []
                        {:config (read-config (io/resource "{{name}}-config.edn"))

                         :handler (-> make-handler
                                      ys/without-lifecycle)

                         :web-server (-> aleph/with-server
                                         (ys/using {:handler [:handler]
                                                    :server-opts [:config :webserver-opts]}))}))

      (ys/with-system-put-to 'user/system)))

(defn -main []
  (nrepl/start-nrepl! {:port 7888})

  (yoyo/set-system-fn! '{{name}}.service.main/make-system)

  (yoyo/start!))
