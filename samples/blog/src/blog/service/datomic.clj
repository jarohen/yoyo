(ns blog.service.datomic
  (:require [clojure.java.io :as io]
            [datomic.api :as d]))

(defn open-datomic-conn [{{:keys [uri]} :datomic-opts}]
  (d/create-database uri)
  (d/connect uri))
