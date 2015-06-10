(ns yoyo.http-kit
  (:require [org.httpkit.server :as http]
            [clojure.tools.logging :as log]))

(defn with-webserver [{:keys [handler port httpkit-opts] :as this} f]
  (log/infof "Starting web server on port %d..." port)
  (let [stop-server! (http/run-server (some-fn handler
                                               (constantly {:status 404
                                                            :body "Not found"}))

                                      (merge {:port port}
                                             httpkit-opts))]
    (log/info "Started web server.")
    (try
      (f this)
      (finally
        (log/info "Stopping web server...")
        (stop-server!)
        (log/info "Stopped web server.")))))
