(ns yoyo.aleph
  (:require [aleph.http :as http]
            [clojure.tools.logging :as log]))

(defn with-webserver [{:keys [handler port aleph-opts] :as this} f]
  (log/infof "Starting web server on port %d..." port)
  (let [server (http/start-server (some-fn handler
                                           (constantly {:status 404
                                                        :body "Not found"}))

                                  (merge {:port port}
                                         aleph-opts))]
    (log/info "Started web server.")
    (try
      (f server)
      (finally
        (log/info "Stopping web server...")
        (.close server)
        (log/info "Stopped web server.")))))
