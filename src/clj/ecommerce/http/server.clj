(ns ecommerce.http.server
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :as jetty]
            [ecommerce.http.routes :as routes]))

(defmethod ig/init-key :ecommerce.adapters.http/server [_ {:keys [port db join?]}]
  (let [handler (routes/routes db)]
    (jetty/run-jetty handler {:port port :join? join?})))

(defmethod ig/halt-key! :ecommerce.adapters.http/server [_ server]
  (.stop server))