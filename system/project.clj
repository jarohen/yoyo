(defproject jarohen/yoyo.system "0.0.1-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                 [medley "0.6.0"]
                 [com.stuartsierra/dependency "0.1.1"]]

  :profiles {:dev {:dependencies [[jarohen/yoyo "0.0.3"]
                                  [com.stuartsierra/component "0.2.3"]]}})
