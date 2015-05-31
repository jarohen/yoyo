(defproject {{name}} ""

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/tools.reader "0.9.2"]
                 [jarohen/embed-nrepl "0.1.0"]

                 [jarohen/yoyo "0.1.0-SNAPSHOT"]

                 [ring/ring-core "1.3.2"]
                 [jarohen/yoyo.aleph "0.1.0-SNAPSHOT"]
                 [bidi "1.19.0"]
                 [hiccup "1.0.5"]
                 [garden "1.2.1"]
                 [ring-middleware-format "0.5.0" :exclusions [ring]]

                 [jarohen/yoyo.cljs "0.1.0-SNAPSHOT"]

                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.9"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]
                 [org.apache.logging.log4j/log4j-core "2.1"]]

  :exclusions [org.clojure/clojure
               org.clojure/clojurescript]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-3269"]
                                  {{#flow?}}[jarohen/flow "0.3.0-alpha3"]{{/flow?}}{{#om?}}[org.omcljs/om "0.8.8"]{{#sablono?}}
                                  [sablono "0.3.4"]{{/sablono?}}{{/om?}}{{#reagent?}}[reagent "0.5.0"]{{/reagent?}}]}}

  :auto-clean false

  :filespecs [{:type :path, :path "target/cljs/build/mains"}]

  :aliases {"dev" ["with-profile" "+dev" "run" "-m" "{{name}}.service.main"]
            "build" ["do"
                     "clean"
                     ["with-profile" "+dev" "run" "-m" "{{name}}.service.main/build!"]
                     "uberjar"]})
