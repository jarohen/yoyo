(ns {{name}}.service.cljs
  (:require [yoyo.cljs :as cljs]
            [yoyo.system :as ys]))

(def cljs-config
  {:source-paths ["ui-src"]

   :main '{{name}}.ui.app

   :web-context-path "/js"

   :output-dir "target/cljs/"

   :dev {:optimizations :none
         :pretty-print? true}

   :build {:optimizations :advanced
           :pretty-print? false
           :classpath-prefix "js"}})

(defn make-cljs-compiler []
  (ys/->dep
   (cljs/start-cljs-compiler! cljs-config)))

(defn build-cljs! []
  (cljs/build-cljs! cljs-config))
