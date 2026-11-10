(ns ecommerce.core
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [ecommerce.db.core]
            [ecommerce.http.server])
  (:gen-class))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defmethod ig/init-key :ecommerce.config/env [_ env]
  env)

(defn read-config []
  (aero/read-config (io/resource "config.edn")))

(defn -main [& _]
  (let [config (read-config)
        system (ig/init config)]
    (println "Server started on port 8080")
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. #(ig/halt! system)))))