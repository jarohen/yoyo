(ns yoyo.jdbc-pool
  (:require [clojure.tools.logging :as log])
  (:import [org.apache.commons.dbcp2 BasicDataSource]))

(def known-drivers
  {"postgresql" "org.postgresql.Driver"
   "mysql" "com.mysql.jdbc.Driver"
   "mssql" "com.microsoft.sqlserver.jdbc.SQLServerDriver"
   "odbc" "sun.jdbc.odbc.JdbcOdbcDriver"
   "sqlite" "org.sqlite.JDBC"
   "h2" "org.h2.Driver"})

(defn with-db-pool [{{:keys [driver subprotocol host port username password db max-total max-idle]} :db-config} f]
  (log/info "Starting JDBC pool...")

  (let [pool {:datasource (doto (BasicDataSource.)
                            (.setDriverClassName (or driver (get known-drivers subprotocol)))
                            (.setAccessToUnderlyingConnectionAllowed true)
                            (.setUrl (format "jdbc:%s://%s:%s/%s" subprotocol host port db))
                            (.setUsername username)
                            (.setPassword password)
                            (cond-> max-total (.setMaxTotal max-total))
                            (cond-> max-idle (.setMaxIdle max-idle)))}]
    (try
      (f pool)

      (finally
        (.close (:datasource pool))))))
