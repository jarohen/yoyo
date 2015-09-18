(ns yoyo.http-kit
  (:require [yoyo.core :as yc]
            [org.httpkit.server :as http]
            [clojure.tools.logging :as log]))

(defn start-server!
  " :: {server-opts} -> Component {server-opts}

  This returns a Component containing a started HTTP-kit server

  Usage:
    (require '[yoyo.http-kit :refer [start-server!]]
             '[yoyo.core :as yc])

    (yc/with-component (start-server! {...})
      (fn [server-opts]
        ;; ...
        ))

  The returned component is also a monadic value compatible with
  Cats's monad functions/macros, and Yoyo's 'system'.

  - handler : a ring handler
  - server-opts : {:port 3000
                   ... ; anything else you'd pass to org.httpkit.server/run-server
                  }

  If the handler returns `nil` for a given request, will return a simple 404.

  The returned component is also a monadic value compatible with
  Cats's monad functions/macros, and Yoyo's 'system'."

  [{:keys [handler server-opts] :as http-kit-opts}]

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
