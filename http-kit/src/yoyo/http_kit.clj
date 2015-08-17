(ns yoyo.http-kit
  (:require [yoyo.core :as yc]
            [org.httpkit.server :as http]
            [clojure.tools.logging :as log]))

(defn start-server! [{:keys [handler server-opts] :as http-kit-opts}]
  (log/infof "Starting web server on port %d..." (:port server-opts))
  (let [stop-server! (http/run-server (some-fn handler
                                               (constantly {:status 404
                                                            :body "Not found"}))

                                      server-opts)]
    (log/info "Started web server.")
    (yc/->component http-kit-opts
                    (fn []
                      (log/info "Stopping web server...")
                      (stop-server!)
                      (log/info "Stopped web server.")))))
