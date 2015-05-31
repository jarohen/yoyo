(ns {{name}}.service.cljs
  (:require [yoyo.cljs :as cljs]))

(def cljs-config
  {:source-path "ui-src"

   :main '{{name}}.ui.app

   :web-context-path "/js"

   :output-dir "target/cljs/"

   :dev {:optimizations :none
         :pretty-print? true}

   :build {:optimizations :advanced
           :pretty-print? false
           :classpath-prefix "js"}})

(defn with-cljs-compiler [f]
  (cljs/with-cljs-compiler cljs-config f))

(defn build-cljs! []
  (cljs/build-cljs! cljs-config))
