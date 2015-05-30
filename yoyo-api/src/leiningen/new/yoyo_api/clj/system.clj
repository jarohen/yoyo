(ns {{name}}.service.system
  (:require [{{name}}.service.handler :refer [make-handler]]
            [clojure.tools.logging :as log]
            [yoyo.aleph :as aleph]))

(defn make-system [latch]
  (aleph/with-webserver {:handler (make-handler)
                         :port 3000}
    (fn [_]
      (latch))))
