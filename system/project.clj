(defproject jarohen/yoyo.system "0.0.1-alpha1"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [medley "0.6.0"]
                 [com.stuartsierra/dependency "0.1.1"]
                 [prismatic/schema "0.4.3"]]

  :profiles {:dev {:dependencies [[jarohen/yoyo "0.0.4"]]}})
