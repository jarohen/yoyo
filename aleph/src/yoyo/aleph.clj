(ns yoyo.aleph
  (:require [yoyo.core :as yc]
            [aleph.http :as http]
            [clojure.tools.logging :as log]))

(defn start-server! [{:keys [handler server-opts]}]
  (log/infof "Starting web server on port %d..." (:port server-opts))
  (let [server (http/start-server (some-fn handler
                                           (constantly {:status 404
                                                        :body "Not found"}))

                                  server-opts)]
    (log/info "Started web server.")

    (yc/->component server
                    (fn []
                      (log/info "Stopping web server...")
                      (.close server)
                      (log/info "Stopped web server.")))))
