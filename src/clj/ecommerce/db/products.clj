(ns ecommerce.db.products
  (:require [clojure.string :as str]
            [honey.sql :as hsql]
            [honey.sql.helpers :as h]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(def db-opts {:builder-fn rs/as-unqualified-kebab-maps})

(defn ->db-product [p]
  {:sku (:sku p)
   :name (:name p)
   :description (or (:description p) "")
   :category (:category p)
   :price (bigdec (or (:price p) 0))
   :stock (int (or (:stock p) 0))
   :weight_kg (bigdec (or (:weight-kg p) (:weight_kg p) 0))
   :updated_at [:raw "NOW()"]})

(defn upsert-products! [db products]
  (when (seq products)
    (let [values (mapv ->db-product products)
          query (-> (h/insert-into :products)
                    (h/values values)
                    (h/on-conflict :sku)
                    (h/do-update-set :name :description :category :price :stock :weight_kg {:updated_at [:raw "NOW()"]})
                    (hsql/format))]
      (jdbc/execute! db query))))

(defn get-all [db]
  (jdbc/execute! db
                 ["SELECT * FROM products ORDER BY updated_at DESC, created_at DESC"]
                 db-opts))

(defn get-by-id [db id]
  (jdbc/execute-one! db
                     ["SELECT * FROM products WHERE id = ?" id]
                     db-opts))

(defn search [db term]
  (let [cleaned (str/trim (str term))]
    (if (str/blank? cleaned)
      (get-all db)
      (let [like-pattern (str "%" cleaned "%")]
        (jdbc/execute! db
                       ["SELECT * FROM products
                         WHERE to_tsvector('english', coalesce(name, '') || ' ' || coalesce(sku, '') || ' ' || coalesce(description, ''))
                               @@ plainto_tsquery('english', ?)
                            OR name ILIKE ?
                            OR sku ILIKE ?
                            OR category ILIKE ?
                            OR coalesce(description, '') ILIKE ?
                         ORDER BY updated_at DESC, created_at DESC"
                        cleaned like-pattern like-pattern like-pattern like-pattern]
                       db-opts)))))

(defn create! [db product]
  (let [row (->db-product product)]
    (jdbc/execute-one! db
                       ["INSERT INTO products (sku, name, description, category, price, stock, weight_kg)
                         VALUES (?, ?, ?, ?, ?, ?, ?)
                         RETURNING *"
                        (:sku row)
                        (:name row)
                        (:description row)
                        (:category row)
                        (:price row)
                        (:stock row)
                        (:weight_kg row)]
                       db-opts)))

(defn update! [db id product]
  (let [row (->db-product product)]
    (jdbc/execute-one! db
                       ["UPDATE products
                         SET sku = ?,
                             name = ?,
                             description = ?,
                             category = ?,
                             price = ?,
                             stock = ?,
                             weight_kg = ?,
                             updated_at = NOW()
                         WHERE id = ?
                         RETURNING *"
                        (:sku row)
                        (:name row)
                        (:description row)
                        (:category row)
                        (:price row)
                        (:stock row)
                        (:weight_kg row)
                        id]
                       db-opts)))

(defn delete! [db id]
  (jdbc/with-transaction [tx db]
    (jdbc/execute-one! tx ["DELETE FROM order_items WHERE product_id = ?" id])
    (jdbc/execute-one! tx
                       ["DELETE FROM products WHERE id = ? RETURNING id" id]
                       db-opts)))
