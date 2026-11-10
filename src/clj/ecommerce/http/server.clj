(ns ecommerce.http.server
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :as jetty]
            [ecommerce.http.routes :as routes]))

(defmethod ig/init-key :ecommerce.adapters.http/server [_ {:keys [port db join?]}]
  (let [parsed-port (if (string? port) (Integer/parseInt port) (int port))
        handler (routes/routes db)]
    (jetty/run-jetty handler {:port parsed-port :join? (boolean join?)})))

(defmethod ig/halt-key! :ecommerce.adapters.http/server [_ server]
  (.stop server))