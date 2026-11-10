(ns ecommerce.db.core
  (:require [integrant.core :as ig]
            [migratus.core :as migratus]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defmethod ig/init-key :ecommerce.adapters.db/pool [_ {:keys [jdbc-url username password]}]
  (let [ds (connection/->pool HikariDataSource {:jdbcUrl jdbc-url
                                                :username username
                                                :password password
                                                :driverClassName "org.postgresql.Driver"})]
    (migratus/migrate {:store :database
                       :migration-dir "migrations"
                       :db {:datasource ds}})
    ds))

(defmethod ig/halt-key! :ecommerce.adapters.db/pool [_ ^HikariDataSource datasource]
  (.close datasource))