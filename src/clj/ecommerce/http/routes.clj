(ns ecommerce.http.routes
  (:require [reitit.ring :as ring]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.ring.coercion :as coercion]
            [reitit.coercion.malli :as malli-coercion]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja-mw]
            [muuntaja.core :as m]
            [ecommerce.http.handlers :as handlers]))

(defn routes [db]
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc true
             :swagger {:info {:title "E-Commerce API"}}
             :handler (swagger/create-swagger-handler)}}]
     ["/api"
      ["/products"
       ["" {:get {:handler (partial handlers/list-products db)}}]
       ["/search" {:get {:parameters {:query [:map [:q :string]]}
                         :handler (partial handlers/search-products db)}}]
       ["/import" {:post {:handler (partial handlers/import-csv db)}}]]]]
    {:data {:coercion malli-coercion/coercion
            :muuntaja m/instance
            :middleware [swagger/swagger-feature
                         parameters/parameters-middleware
                         muuntaja-mw/format-middleware
                         multipart/multipart-middleware
                         coercion/coerce-exceptions-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler {:path "/api/docs"})
    (ring/create-default-handler))))