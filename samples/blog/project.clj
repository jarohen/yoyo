(defproject blog ""

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.reader "0.9.2"]
                 [jarohen/embed-nrepl "0.1.1"]

                 [jarohen/yoyo "0.0.4"]
                 [jarohen/yoyo.system "0.0.1-alpha1"]

                 [ring/ring-core "1.3.2"]
                 [jarohen/yoyo.aleph "0.0.3"]
                 [jarohen.forks/bidi "1.20.1-SNAPSHOT"]
                 [hiccup "1.0.5"]
                 [garden "1.2.1"]
                 [ring-middleware-format "0.5.0" :exclusions [ring]]

                 [org.webjars/jquery "2.1.4"]
                 [org.webjars/bootstrap "3.3.5"]

                 [jarohen/yoyo.cljs "0.0.4"]

                 [jarohen/nomad "0.8.0-beta3"]

                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.9"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]
                 [org.apache.logging.log4j/log4j-core "2.1"]]

  :source-paths ["src" "common-src"]

  :exclusions [org.clojure/clojure
               org.clojure/clojurescript]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-3269"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [weasel "0.7.0"]
                                  [reagent "0.5.0"]]}}

  :auto-clean false

  :filespecs [{:type :path, :path "target/cljs/build/mains"}]

  :main ^:skip-aot blog.service.main

  :aliases {"dev" "run"
            "build" ["do"
                     "clean"
                     ["run" "-m" "blog.service.main/build!"]
                     "uberjar"]})
