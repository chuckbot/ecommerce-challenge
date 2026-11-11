(ns ecommerce.domain.orders)

(defn normalize-items [items]
  (->> items
       (filter #(and (:product-id %)
                     (int? (:quantity %))
                     (pos? (:quantity %))))
       (group-by :product-id)
       (map (fn [[pid group]]
              {:product-id pid
               :quantity (reduce + 0 (map :quantity group))}))
       (sort-by (comp str :product-id))
       vec))

(defn validate-and-build-line-item [product {:keys [product-id quantity]}]
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
  (let [unit-price (bigdec (:price product))
        unit-weight (bigdec (:weight-kg product))
        qty-dec (bigdec quantity)]
    {:product product
     :quantity quantity
     :unit-price unit-price
     :line-amount (.multiply unit-price qty-dec)
     :line-weight (.multiply unit-weight qty-dec)}))

(defn calculate-order-totals [line-items]
  {:total-amount (reduce #(.add ^BigDecimal %1 ^BigDecimal %2)
                         (bigdec 0)
                         (map :line-amount line-items))
   :total-weight-kg (reduce #(.add ^BigDecimal %1 ^BigDecimal %2)
                            (bigdec 0)
                            (map :line-weight line-items))})
