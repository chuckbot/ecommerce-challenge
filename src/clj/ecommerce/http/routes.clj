(ns ecommerce.http.routes
  (:require [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.coercion :as coercion]
            [reitit.coercion.malli :as malli-coercion]
            [reitit.ring.middleware.parameters :as parameters]
            [ecommerce.schema :as schema]))

(defn health-handler [_]
  {:status 200
   :body {:status "ok"
          :timestamp (System/currentTimeMillis)}})

(defn routes [db]
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc true
             :swagger {:info {:title "E-Commerce API"
                              :description "Enterprise coding challenge API"}}
             :handler (swagger/create-swagger-handler)}}]
     ["/api"
      ["/health" {:get {:handler health-handler}}]
      ["/products"
       ["/import" {:post {:summary "Import products from CSV"
                          :handler (fn [_] {:status 501 :body "Not implemented"})}}]]]]
    {:data {:coercion malli-coercion/coercion
            :middleware [swagger/swagger-feature
                         parameters/parameters-middleware
                         coercion/coerce-exceptions-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler {:path "/api/docs"})
    (ring/create-default-handler))))