(ns ecommerce.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))

(rf/reg-sub
 :products
 (fn [db _]
   (:products db)))

(rf/reg-sub
 :category-filter
 (fn [db _]
   (:category-filter db)))

(rf/reg-sub
 :categories
 :<- [:products]
 (fn [products _]
   (->> products
        (keep :category)
        distinct
        sort
        vec)))

(rf/reg-sub
 :filtered-products
 :<- [:products]
 :<- [:category-filter]
 (fn [[products category-filter] _]
   (if (or (nil? category-filter) (= category-filter "ALL"))
     products
     (filterv #(= (:category %) category-filter) products))))

(rf/reg-sub
 :loading?
 (fn [db _]
   (:loading? db)))

(rf/reg-sub
 :search-term
 (fn [db _]
   (:search-term db)))

(rf/reg-sub
 :cart
 (fn [db _]
   (:cart db)))

(rf/reg-sub
 :cart-items
 :<- [:cart]
 (fn [cart _]
   (vec (vals cart))))

(rf/reg-sub
 :cart-summary
 :<- [:cart-items]
 (fn [items _]
   (reduce (fn [acc {:keys [product quantity]}]
             (let [price (js/parseFloat (str (:price product)))
                   weight (js/parseFloat (str (:weight-kg product)))]
               (-> acc
                   (update :count + quantity)
                   (update :total-amount + (* price quantity))
                   (update :total-weight + (* weight quantity)))))
           {:count 0 :total-amount 0.0 :total-weight 0.0}
           items)))

(rf/reg-sub
 :orders
 (fn [db _]
   (:orders db)))

(rf/reg-sub
 :last-order
 (fn [db _]
   (:last-order db)))

(rf/reg-sub
 :import-status
 (fn [db _]
   (:import-status db)))

(rf/reg-sub
 :importing?
 (fn [db _]
   (:importing? db)))

(rf/reg-sub
 :product-modal
 (fn [db _]
   (:product-modal db)))

(rf/reg-sub
 :notification
 (fn [db _]
   (:notification db)))
