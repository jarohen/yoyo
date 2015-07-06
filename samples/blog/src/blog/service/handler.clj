(ns blog.service.handler
  (:require [blog.service.css :as css]
            [blog.api-routes :refer [api-routes]]
            [bidi.bidi :as bidi]
            [bidi.ring :as br]
            [hiccup.page :refer [html5 include-css include-js]]
            [ring.util.response :refer [response content-type]]
            [yoyo.cljs :as cljs]))

(def site-routes
  ["" {"/" {:get :page-handler}
       "/css" {"/site.css" {:get :site-css}}
       "/webjars" (br/resources {:prefix "META-INF/resources/webjars"})}])

(defn page-handler [{:keys [cljs-compiler]}]
  (fn [req]
    (-> (response
         (html5
          [:head
           [:title "My Blog!"]

           (include-css "/webjars/bootstrap/3.3.5/css/bootstrap.min.css")

           (include-js (cljs/path-for-js cljs-compiler))
           (include-css (bidi/path-for site-routes :site-css :request-method :get))]

          [:body]))

        (content-type "text/html"))))

(defn site-handlers [handler-opts]
  {:page-handler (page-handler handler-opts)
   :site-css (fn [req]
                    (-> (response (css/site-css))
                        (content-type "text/css")))})

(defn api-handlers []
  {})

(defn make-handler [{:keys [cljs-compiler] :as handler-opts}]
  (br/make-handler ["" [site-routes
                        (api-routes)
                        (cljs/bidi-routes cljs-compiler)]]

                   (some-fn (site-handlers handler-opts)
                            (api-handlers)

                            #(when (fn? %) %)

                            (constantly {:status 404
                                         :body "Not found."}))))
