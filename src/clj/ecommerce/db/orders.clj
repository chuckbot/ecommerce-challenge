(ns ecommerce.db.orders
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(def db-opts {:builder-fn rs/as-unqualified-kebab-maps})

(defn normalize-items [items]
  (->> items
       (group-by :product-id)
       (map (fn [[pid group]]
              {:product-id pid
               :quantity (reduce + 0 (map :quantity group))}))
       (sort-by (comp str :product-id))
       vec))

(defn create-order! [db items]
  (let [normalized (normalize-items items)]
    (jdbc/with-transaction [tx db]
      (let [locked-items
            (mapv (fn [{:keys [product-id quantity]}]
                    (let [product (jdbc/execute-one!
                                   tx
                                   ["SELECT * FROM products WHERE id = ? FOR UPDATE" product-id]
                                   db-opts)]
                      (when-not product
                        (throw (ex-info (str "Product not found: " product-id)
                                        {:type :not-found
                                         :product-id product-id})))
                      (when (< (:stock product) quantity)
                        (throw (ex-info (str "Insufficient stock for " (:name product)
                                             " (available: " (:stock product)
                                             ", requested: " quantity ")")
                                        {:type :insufficient-stock
                                         :product-id product-id
                                         :sku (:sku product)
                                         :name (:name product)
                                         :available (:stock product)
                                         :requested quantity})))
                      {:product product
                       :quantity quantity
                       :line-amount (.multiply (bigdec (:price product)) (bigdec quantity))
                       :line-weight (.multiply (bigdec (:weight-kg product)) (bigdec quantity))}))
                  normalized)
            total-amount (reduce #(.add ^BigDecimal %1 ^BigDecimal %2)
                                 (bigdec 0)
                                 (map :line-amount locked-items))
            total-weight (reduce #(.add ^BigDecimal %1 ^BigDecimal %2)
                                 (bigdec 0)
                                 (map :line-weight locked-items))
            order (jdbc/execute-one!
                   tx
                   ["INSERT INTO orders (total_amount, total_weight_kg, payment_status)
                     VALUES (?, ?, 'COMPLETED')
                     RETURNING *"
                    total-amount total-weight]
                   db-opts)
            order-items
            (mapv (fn [{:keys [product quantity]}]
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
                                 (bigdec (:price product))]
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
