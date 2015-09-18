(ns {{name}}.service.main
  (:require [{{name}}.service.handler :refer [m-make-handler]]
            [{{name}}.service.cljs :as cljs]
            [cats.core :as c]
            [clojure.tools.logging :as log]
            [nrepl.embed :as nrepl]
            [yoyo :as y]
            [yoyo.aleph :as aleph]
            [yoyo.system :as ys]))

(defn make-webserver []
  (c/mlet [handler (m-make-handler)]
    (ys/->dep
     (aleph/start-server! {:handler handler
                           :server-opts {:port 3000}}))))

(defn make-system []
  (ys/make-system #{(-> cljs/make-cljs-compiler
                        (ys/named :cljs-compiler))

                    (-> make-webserver
                        (ys/named :web-server))}))

(defn build! []
  (cljs/build-cljs!)
  (System/exit 0))

(defn -main []
  (nrepl/start-nrepl! {:port 7888})

  (y/set-system-fn! '{{name}}.service.main/make-system)

  (y/start!))
