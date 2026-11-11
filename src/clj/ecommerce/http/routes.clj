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
                              :description "Hexagonal Architecture E-Commerce REST API (Products CRUD, Fault-Tolerant CSV Import, Full-Text Search, and Atomic Orders)"
                              :version "1.0.0"}}
             :handler (swagger/create-swagger-handler)}}]
     ["/api"
      ["/products"
       {:swagger {:tags ["Products"]}}
       ["" {:get {:summary "List all products ordered by most recently updated"
                  :handler (partial handlers/list-products db)}
            :post {:summary "Create a new product in the catalog"
                   :handler (partial handlers/create-product db)}}]
       ["/search" {:get {:summary "Search products via PostgreSQL Full-Text Search and ILIKE"
                         :parameters {:query [:map [:q {:optional true} :string]]}
                         :handler (partial handlers/search-products db)}}]
       ["/import" {:post {:summary "Import products from a multipart uploaded CSV file"
                          :handler (partial handlers/import-csv db)}}]
       ["/import-sample" {:post {:summary "Import products from the bundled challenge sample CSV"
                                 :handler (partial handlers/import-sample-csv db)}}]
       ["/:id" {:put {:summary "Update an existing product by UUID"
                      :handler (partial handlers/update-product db)}
                :delete {:summary "Delete a product by UUID"
                         :handler (partial handlers/delete-product db)}}]]
      ["/orders"
       {:swagger {:tags ["Orders"]}}
       ["" {:get {:summary "List recent completed purchase orders with line items"
                  :handler (partial handlers/list-orders db)}
            :post {:summary "Execute an atomic purchase transaction (SELECT ... FOR UPDATE)"
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
