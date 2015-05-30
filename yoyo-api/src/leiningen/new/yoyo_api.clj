(ns leiningen.new.yoyo-api
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]))

(def render (renderer "yoyo-api"))

(defn yoyo-api
  "Create a new Yo-yo API"
  [app-name]

  (println "Creating a new Yo-yo API...")

  (let [data {:name app-name
              :sanitized (name-to-path app-name)}]

    (->files data
             ["project.clj" (render "project.clj" data)]
             [".gitignore" (render "gitignore" data)]
             ["resources/log4j2.json" (render "resources/log4j2.json" data)]

             ["src/{{sanitized}}/service/main.clj" (render "clj/main.clj" data)]
             ["src/{{sanitized}}/service/system.clj" (render "clj/system.clj" data)]

             ["src/{{sanitized}}/service/handler.clj" (render "clj/handler.clj" data)]))

  (println "Created!")
  (println "To start the application, run `lein dev`, and then go to http://localhost:3000"))
