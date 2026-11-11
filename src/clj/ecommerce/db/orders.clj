(ns ecommerce.db.orders
  (:require [ecommerce.domain.orders :as domain.orders]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(def db-opts {:builder-fn rs/as-unqualified-kebab-maps})

(defn create-order! [db items]
  (let [normalized (domain.orders/normalize-items items)]
    (jdbc/with-transaction [tx db]
      (let [locked-items
            (mapv (fn [{:keys [product-id] :as item}]
                    (let [product (jdbc/execute-one!
                                   tx
                                   ["SELECT * FROM products WHERE id = ? FOR UPDATE" product-id]
                                   db-opts)]
                      (domain.orders/validate-and-build-line-item product item)))
                  normalized)
            {:keys [total-amount total-weight-kg]} (domain.orders/calculate-order-totals locked-items)
            order (jdbc/execute-one!
                   tx
                   ["INSERT INTO orders (total_amount, total_weight_kg, payment_status)
                     VALUES (?, ?, 'COMPLETED')
                     RETURNING *"
                    total-amount total-weight-kg]
                   db-opts)
            order-items
            (mapv (fn [{:keys [product quantity unit-price]}]
                    (jdbc/execute-one!
                     tx
                     ["UPDATE products
                       SET stock = stock - ?,
                           updated_at = NOW()
                       WHERE id = ?"
                      quantity (:id product)])
                    (let [item (jdbc/execute-one!
                                tx
                                ["INSERT INTO order_items (order_id, product_id, sku, quantity, unit_price)
                                  VALUES (?, ?, ?, ?, ?)
                                  RETURNING *"
                                 (:id order)
                                 (:id product)
                                 (:sku product)
                                 quantity
                                 unit-price]
                                db-opts)]
                      (assoc item :product-name (:name product))))
                  locked-items)]
        (assoc order :items order-items)))))

(defn get-all [db]
  (let [orders (jdbc/execute!
                db
                ["SELECT * FROM orders ORDER BY created_at DESC LIMIT 50"]
                db-opts)
        items (jdbc/execute!
               db
               ["SELECT oi.*, p.name AS product_name
                 FROM order_items oi
                 LEFT JOIN products p ON p.id = oi.product_id
                 ORDER BY oi.id"]
               db-opts)
        items-by-order (group-by :order-id items)]
    (mapv (fn [o]
            (assoc o :items (get items-by-order (:id o) [])))
          orders)))
