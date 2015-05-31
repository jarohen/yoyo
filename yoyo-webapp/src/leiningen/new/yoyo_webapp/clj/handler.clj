(ns {{name}}.service.handler
  (:require [{{name}}.service.css :as css]
            [bidi.bidi :as bidi]
            [bidi.ring :as br]
            [yoyo.cljs :as cljs]
            [hiccup.page :refer [html5 include-css include-js]]
            [ring.util.response :refer [response content-type]]))

;; This is all in one NS for now, but you'll likely want to split it
;; out when your webapp grows!

(def site-routes
  ["" {"/" {:get :page-handler}
       "/css" {"/site.css" {:get :site-css}}}])

(defn page-handler [{:keys [cljs-compiler]}]
  (fn [req]
    (-> (response
         (html5
          [:head
           [:title "{{name}} - CLJS Single Page Web Application"]

           (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
           (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")

           (include-js (cljs/path-for-js cljs-compiler))
           (include-css (bidi/path-for site-routes :site-css :request-method :get))]

          [:body]))

        (content-type "text/html"))))

(defn site-handlers [handler-opts]
  {:page-handler (page-handler handler-opts)
   :site-css (fn [req]
                    (-> (response (css/site-css))
                        (content-type "text/css")))})

(def api-routes
  ["/api" {}])

(defn api-handlers []
  {})

(defn make-handler [{:keys [cljs-compiler] :as handler-opts}]
  (br/make-handler ["" [site-routes
                        api-routes
                        (cljs/bidi-routes cljs-compiler)]]

                   (some-fn (site-handlers handler-opts)
                            (api-handlers)

                            #(when (fn? %) %)

                            (constantly {:status 404
                                         :body "Not found."}))))
