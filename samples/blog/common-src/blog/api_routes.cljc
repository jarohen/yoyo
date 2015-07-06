(ns blog.api-routes
  (:require [bidi.bidi :as bidi]))

(defn blog-routes []
  {"/blogs" {"/" {:get ::get-blogs}
             ["/" [bidi/uuid :blog-id]] {:get ::get-blog}}})

(defn api-routes []
  {"/api" (blog-routes)})
