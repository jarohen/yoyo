(ns yoyo.http-kit
  (:require [org.httpkit.server :as http]
            [clojure.tools.logging :as log]))

(defn with-server [{:keys [handler server-opts] :as http-kit-opts} f]
  (log/infof "Starting web server on port %d..." port)
  (let [stop-server! (http/run-server (some-fn handler
                                               (constantly {:status 404
                                                            :body "Not found"}))

                                      server-opts)]
    (log/info "Started web server.")
    (try
      (f http-kit-opts)
      (finally
        (log/info "Stopping web server...")
        (stop-server!)
        (log/info "Stopped web server.")))))
