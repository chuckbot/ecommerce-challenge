(ns ecommerce.http.routes
  (:require [clojure.java.io :as io]
            [ecommerce.http.handlers :as handlers]
            [muuntaja.core :as m]
            [reitit.coercion.malli :as malli-coercion]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja-mw]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]))

(defn index-handler [_]
  (if-let [res (io/resource "public/index.html")]
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (slurp res)}
    {:status 404
     :body "index.html not found"}))

(defn routes [db]
  (ring/ring-handler
   (ring/router
    [["/" {:get {:no-doc true
                 :handler index-handler}}]
     ["/swagger.json"
      {:get {:no-doc true
             :swagger {:info {:title "Enterprise E-Commerce API"
                              :version "1.0.0"}}
             :handler (swagger/create-swagger-handler)}}]
     ["/api"
      ["/products"
       ["" {:get {:summary "List all products"
                  :handler (partial handlers/list-products db)}
            :post {:summary "Create a new product"
                   :handler (partial handlers/create-product db)}}]
       ["/search" {:get {:summary "Search products by term"
                         :parameters {:query [:map [:q {:optional true} :string]]}
                         :handler (partial handlers/search-products db)}}]
       ["/import" {:post {:summary "Import products from uploaded CSV file"
                          :handler (partial handlers/import-csv db)}}]
       ["/import-sample" {:post {:summary "Import products from bundled sample CSV"
                                 :handler (partial handlers/import-sample-csv db)}}]
       ["/:id" {:put {:summary "Update a product by ID"
                      :handler (partial handlers/update-product db)}
                :delete {:summary "Delete a product by ID"
                         :handler (partial handlers/delete-product db)}}]]
      ["/orders"
       ["" {:get {:summary "List recent orders"
                  :handler (partial handlers/list-orders db)}
            :post {:summary "Purchase products atomically"
                   :handler (partial handlers/create-order db)}}]]]]
    {:conflicts nil
     :data {:coercion malli-coercion/coercion
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
    (ring/create-resource-handler {:path "/"})
    (ring/create-default-handler))))
