(ns blog.service.cljs
  (:require [yoyo.cljs :as cljs]))

(def cljs-config
  {:source-paths ["ui-src"]

   :main 'blog.ui.app

   :web-context-path "/js"

   :output-dir "target/cljs/"

   :dev {:optimizations :none
         :pretty-print? true}

   :build {:optimizations :advanced
           :pretty-print? false
           :classpath-prefix "js"}})

(defn with-cljs-compiler [_ f]
  (cljs/with-cljs-compiler {:cljs-opts cljs-config} f))

(defn build-cljs! []
  (cljs/build-cljs! cljs-config))
