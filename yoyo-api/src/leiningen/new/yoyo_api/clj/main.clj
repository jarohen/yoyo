(ns {{name}}.service.main
  (:require [{{name}}.service.handler :refer [make-handler]]
            [clojure.tools.logging :as log]
            [nrepl.embed :as nrepl]
            [yoyo]
            [yoyo.aleph :as aleph]))

(defn make-system [latch]
  (aleph/with-webserver {:handler (make-handler)
                         :port 3000}
    (fn [_]
      (latch))))

(defn -main []
  (nrepl/start-nrepl! {:port 7888})

  (yoyo/set-system-fn! '{{name}}.service.main/make-system)

  (yoyo/start!))
