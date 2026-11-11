(ns ecommerce.subs
  (:require [clojure.string :as str]
            [re-frame.core :as rf]))

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
 :sort-by
 (fn [db _]
   (or (:sort-by db) :recent)))

(rf/reg-sub
 :current-page
 (fn [db _]
   (or (:current-page db) 1)))

(rf/reg-sub
 :page-size
 (fn [db _]
   (or (:page-size db) 15)))

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
 :category-counts
 :<- [:products]
 (fn [products _]
   (frequencies (keep :category products))))

(defn sort-products [products sort-key]
  (case sort-key
    :price-asc (vec (sort-by #(js/parseFloat (str (:price %))) products))
    :price-desc (vec (sort-by #(js/parseFloat (str (:price %))) > products))
    :stock-asc (vec (sort-by :stock products))
    :stock-desc (vec (sort-by :stock > products))
    :name-asc (vec (sort-by #(str/lower-case (str (:name %))) products))
    (vec products)))

(rf/reg-sub
 :filtered-products
 :<- [:products]
 :<- [:category-filter]
 :<- [:sort-by]
 (fn [[products category-filter sort-key] _]
   (let [by-cat (if (or (nil? category-filter) (= category-filter "ALL"))
                  products
                  (filterv #(= (:category %) category-filter) products))]
     (sort-products by-cat sort-key))))

(rf/reg-sub
 :pagination
 :<- [:filtered-products]
 :<- [:current-page]
 :<- [:page-size]
 (fn [[filtered current-page page-size] _]
   (let [total-items (count filtered)
         total-pages (max 1 (js/Math.ceil (/ total-items page-size)))
         safe-page (min (max 1 current-page) total-pages)
         start-idx (* (dec safe-page) page-size)
         end-idx (min total-items (+ start-idx page-size))
         items (if (pos? total-items)
                 (subvec filtered start-idx end-idx)
                 [])]
     {:items items
      :total-items total-items
      :total-pages total-pages
      :current-page safe-page
      :page-size page-size
      :start-item (if (pos? total-items) (inc start-idx) 0)
      :end-item end-idx})))

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
