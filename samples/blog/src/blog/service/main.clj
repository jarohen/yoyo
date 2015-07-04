(ns blog.service.main
  (:require [blog.service.cljs :as cljs]
            [blog.service.handler :refer [make-handler]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [nomad :refer [read-config]]
            [nrepl.embed :as nrepl]
            [yoyo.aleph :as aleph]
            [yoyo :as y]
            [yoyo.system :as ys]))

(def make-system
  (-> (ys/make-system (fn []
                        {:config (read-config (io/resource "blog-config.edn"))
                         :cljs-compiler cljs/with-cljs-compiler
                         :handler (-> make-handler
                                      (ys/using {:cljs-compiler [:cljs-compiler]})
                                      ys/without-lifecycle)

                         :web-server (-> aleph/with-server
                                         (ys/using {:handler [:handler]
                                                    :server-opts [:config :webserver-opts]}))}))
      (ys/with-system-put-to 'user/system)))

(defn build! []
  (cljs/build-cljs!)
  (System/exit 0))

(defn -main []
  (nrepl/start-nrepl! {:port 7888})

  (yoyo/set-system-fn! 'blog.service.main/make-system)

  (yoyo/start!))
