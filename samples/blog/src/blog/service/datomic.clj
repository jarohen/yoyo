(ns blog.service.datomic
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [datomic.api :as d]))

(defn assert-schema! [datomic-conn]
  (log/info "Migrating Datomic database...")
  @(d/transact datomic-conn
               (read-string (slurp (io/resource "datomic-schema.edn"))))
  (log/info "Migrated Datomic database."))

(defn open-datomic-conn [{{:keys [uri]} :datomic-opts}]
  (d/create-database uri)
  (doto (d/connect uri)
    assert-schema!))
