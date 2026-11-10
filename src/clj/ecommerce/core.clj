(ns ecommerce.core
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [ecommerce.db.core]
            [ecommerce.http.server])
  (:gen-class))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defn read-config []
  (aero/read-config (io/resource "config.edn")))

(defn -main [& _]
  (let [config (read-config)]
    (ig/load-namespaces config)
    (ig/init config)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. #(ig/halt!)))))