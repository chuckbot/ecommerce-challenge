(ns ecommerce.http.handlers
  (:require [clojure.java.io :as io]
            [ecommerce.db.orders :as db.orders]
            [ecommerce.db.products :as db.products]
            [ecommerce.domain.csv-import :as csv-import]
            [ecommerce.schema :as schema])
  (:import (java.util UUID)
           (org.postgresql.util PSQLException)))

(defn parse-uuid-safe [v]
  (cond
    (uuid? v) v
    (string? v) (try
                  (UUID/fromString v)
                  (catch IllegalArgumentException _ nil))
    :else nil))

(defn run-csv-import! [db file-source]
  (let [result (csv-import/process-file file-source)]
    (when (seq (:valid result))
      (db.products/upsert-products! db (:valid result)))
    {:status 200
     :body {:imported (count (:valid result))
            :total-valid-rows (or (:total-valid-rows result) (count (:valid result)))
            :duplicates-merged (count (:duplicates result))
            :duplicates (or (:duplicates result) [])
            :failed (count (:invalid result))
            :skipped (:skipped result)
            :errors (:invalid result)}}))

(defn import-csv [db request]
  (let [file (or (get-in request [:parameters :multipart :file :tempfile])
                 (get-in request [:multipart-params "file" :tempfile]))]
    (if-not file
      {:status 400 :body {:error "No file provided in 'file' field"}}
      (run-csv-import! db file))))

(defn import-sample-csv [db _]
  (let [sample-file (io/file "Code Challenge E-Commerce.csv")]
    (if (.exists sample-file)
      (run-csv-import! db sample-file)
      {:status 404 :body {:error "Sample file 'Code Challenge E-Commerce.csv' not found on server"}})))

(defn list-products [db _]
  {:status 200
   :body (db.products/get-all db)})

(defn search-products [db request]
  (let [term (or (get-in request [:parameters :query :q])
                 (get-in request [:query-params "q"])
                 "")]
    {:status 200
     :body (db.products/search db term)}))

(defn create-product [db request]
  (let [payload (or (:body-params request) (get-in request [:parameters :body]) {})
        validation (schema/validate-product-input payload)]
    (if-not (:valid? validation)
      {:status 400
       :body {:error "Invalid product data"
              :details (:errors validation)}}
      (try
        (let [created (db.products/create! db (:data validation))]
          {:status 201 :body created})
        (catch PSQLException e
          (if (= "23505" (.getSQLState e))
            {:status 409 :body {:error (str "A product with SKU '" (get-in validation [:data :sku]) "' already exists")}}
            {:status 500 :body {:error (.getMessage e)}}))))))

(defn update-product [db request]
  (let [raw-id (or (get-in request [:path-params :id])
                   (get-in request [:parameters :path :id]))
        id (parse-uuid-safe raw-id)
        payload (or (:body-params request) (get-in request [:parameters :body]) {})
        validation (schema/validate-product-input payload)]
    (cond
      (nil? id)
      {:status 400 :body {:error "Invalid product UUID"}}

      (not (:valid? validation))
      {:status 400
       :body {:error "Invalid product data"
              :details (:errors validation)}}

      :else
      (try
        (if-let [updated (db.products/update! db id (:data validation))]
          {:status 200 :body updated}
          {:status 404 :body {:error "Product not found"}})
        (catch PSQLException e
          (if (= "23505" (.getSQLState e))
            {:status 409 :body {:error (str "Another product with SKU '" (get-in validation [:data :sku]) "' already exists")}}
            {:status 500 :body {:error (.getMessage e)}}))))))

(defn delete-product [db request]
  (let [raw-id (or (get-in request [:path-params :id])
                   (get-in request [:parameters :path :id]))
        id (parse-uuid-safe raw-id)]
    (if-not id
      {:status 400 :body {:error "Invalid product UUID"}}
      (if (db.products/delete! db id)
        {:status 200 :body {:deleted true :id id}}
        {:status 404 :body {:error "Product not found"}}))))

(defn create-order [db request]
  (let [raw-items (or (get-in request [:body-params :items])
                      (get-in request [:parameters :body :items])
                      [])
        items (->> raw-items
                   (map (fn [item]
                          {:product-id (parse-uuid-safe (or (:product-id item) (:product_id item)))
                           :quantity (schema/parse-clean-int (:quantity item))}))
                   (filter #(and (:product-id %) (int? (:quantity %)) (pos? (:quantity %))))
                   vec)]
    (if (empty? items)
      {:status 400 :body {:error "Order must include at least one valid item with quantity > 0"}}
      (try
        (let [order (db.orders/create-order! db items)]
          {:status 201 :body order})
        (catch clojure.lang.ExceptionInfo e
          (let [data (ex-data e)]
            (case (:type data)
              :not-found {:status 404 :body {:error (ex-message e) :details data}}
              :insufficient-stock {:status 409 :body {:error (ex-message e) :details data}}
              {:status 400 :body {:error (ex-message e)}})))))))

(defn list-orders [db _]
  {:status 200
   :body (db.orders/get-all db)})
