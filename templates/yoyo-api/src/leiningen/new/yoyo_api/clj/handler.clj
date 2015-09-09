(ns {{name}}.service.handler
  (:require [bidi.ring :as br]
            [ring.util.response :refer [response]]))

;; This is all in one NS for now, but you'll likely want to split it
;; out when your app grows!

(def api-routes
  {"/" {:get ::default-route}
   "/api" {}})

(defn api-handlers []
  {::default-route (fn [req]
                     (response "Hello world from Yo-yo! Try `(yoyo/reload!)` to completely reload the application"))})

(defn make-handler []
  (br/make-handler ["" api-routes]

                   (some-fn (api-handlers)

                            #(when (fn? %) %)

                            (constantly {:status 404
                                         :body "Not found."}))))
