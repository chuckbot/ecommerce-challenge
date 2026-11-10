(ns ecommerce.db.products
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql :as hsql]
            [honey.sql.helpers :as h]))

(defn upsert-products! [db products]
  (when (seq products)
    (let [values (map #(-> (select-keys % [:sku :name :description :category :price :stock :weight_kg])
                           (assoc :updated_at [:raw "NOW()"]))
                      products)
          query (-> (h/insert-into :products)
                    (h/values values)
                    (h/on-conflict :sku)
                    (h/do-update-set :name :description :category :price :stock :weight_kg {:updated_at [:raw "NOW()"]})
                    (hsql/format))]
      (jdbc/execute! db query))))

(defn get-all [db]
  (jdbc/execute! db ["SELECT * FROM products ORDER BY created_at DESC"]
                 {:builder-fn rs/as-unqualified-kebab-maps}))

(defn search [db term]
  (jdbc/execute! db
                 ["SELECT * FROM products 
                   WHERE to_tsvector('english', coalesce(name, '') || ' ' || coalesce(sku, '') || ' ' || coalesce(description, '')) @@ plainto_tsquery('english', ?) 
                   ORDER BY created_at DESC" term]
                 {:builder-fn rs/as-unqualified-kebab-maps}))