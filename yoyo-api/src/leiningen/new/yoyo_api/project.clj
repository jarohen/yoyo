(defproject {{name}} ""

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [jarohen/embed-nrepl "0.1.1"]

                 [jarohen/yoyo "0.0.4"]
                 [jarohen/yoyo.system "0.0.1-alpha1"]

                 [ring/ring-core "1.3.2"]
                 [jarohen/yoyo.aleph "0.0.3"]
                 [bidi "1.20.0"]
                 [ring-middleware-format "0.5.0" :exclusions [ring]]

                 [jarohen/nomad "0.8.0-beta3"]

                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.9"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]
                 [org.apache.logging.log4j/log4j-core "2.1"]]

  :exclusions [org.clojure/clojure
               org.clojure/clojurescript]

  :main ^:skip-aot {{name}}.service.main

  :aliases {"dev" "run"
            "build" "uberjar"})
