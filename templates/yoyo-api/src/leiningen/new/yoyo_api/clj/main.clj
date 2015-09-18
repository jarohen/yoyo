(ns {{name}}.service.main
  (:require [{{name}}.service.handler :refer [make-handler]]
            [clojure.tools.logging :as log]
            [nrepl.embed :as nrepl]
            [yoyo.aleph :as aleph]
            [yoyo.system :as ys]))

(defn make-web-server []
  (ys/->dep
   (aleph/start-server! {:handler (make-handler)
                         :server-opts {:port 3000}})))

(defn make-system []
  (ys/make-system #{(-> make-web-server
                        (ys/named :web-server))}))

(defn -main []
  (nrepl/start-nrepl! {:port 7888})

  (yoyo/set-system-fn! '{{name}}.service.main/make-system)

  (yoyo/start!))
