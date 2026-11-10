(ns ecommerce.http.handlers
  (:require [ecommerce.domain.csv-import :as csv-import]
            [ecommerce.db.products :as db.products]))

(defn import-csv [db request]
  (let [file (or (get-in request [:parameters :multipart :file :tempfile])
                 (get-in request [:multipart-params "file" :tempfile]))]
    (if-not file
      {:status 400 :body {:error "No file provided in 'file' field"}}
      (let [result (csv-import/process-file file)]
        (when (seq (:valid result))
          (db.products/upsert-products! db (:valid result)))
        {:status 200
         :body {:imported (count (:valid result))
                :failed (count (:invalid result))
                :skipped (:skipped result)
                :errors (:invalid result)}}))))

(defn list-products [db _]
  {:status 200
   :body (db.products/get-all db)})

(defn search-products [db request]
  (let [term (get-in request [:parameters :query :q])]
    (if term
      {:status 200 :body (db.products/search db term)}
      {:status 400 :body {:error "Missing search term 'q'"}})))