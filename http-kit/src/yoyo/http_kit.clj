(ns yoyo.http-kit
  (:require [org.httpkit.server :as http]
            [clojure.tools.logging :as log]))

(defn with-server [{:keys [handler server-opts] :as http-kit-opts}]
  (fn [f]
    (log/infof "Starting web server on port %d..." (:port server-opts))
    (let [stop-server! (http/run-server (some-fn handler
                                                 (constantly {:status 404
                                                              :body "Not found"}))

                                        server-opts)]
      (log/info "Started web server.")
      (f http-kit-opts
         (fn []
           (log/info "Stopping web server...")
           (stop-server!)
           (log/info "Stopped web server."))))))
