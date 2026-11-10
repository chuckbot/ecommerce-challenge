(ns ecommerce.db.core
  (:require [integrant.core :as ig]
            [next.jdbc.connection :as connection])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defmethod ig/init-key :ecommerce.adapters.db/pool [_ {:keys [jdbc-url]}]
  (connection/->pool com.zaxxer.hikari.HikariDataSource {:jdbcUrl jdbc-url}))

(defmethod ig/halt-key! :ecommerce.adapters.db/pool [_ ^HikariDataSource datasource]
  (.close datasource))