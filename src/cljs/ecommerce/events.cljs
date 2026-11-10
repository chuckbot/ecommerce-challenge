(ns ecommerce.events
  (:require [ajax.core :as ajax]
            [ajax.easy :as ajax-easy]
            [clojure.string :as str]
            [re-frame.core :as rf]))

(def empty-product-form
  {:id nil
   :sku ""
   :name ""
   :description ""
   :category ""
   :price ""
   :stock ""
   :weight-kg ""})

(rf/reg-event-fx
 :initialize-db
 (fn [_ _]
   {:db {:products []
         :orders []
         :loading? false
         :search-term ""
         :category-filter "ALL"
         :active-panel :catalog
         :cart {}
         :last-order nil
         :import-status nil
         :importing? false
         :product-modal {:open? false
                         :mode :create
                         :form empty-product-form
                         :error nil}
         :notification nil}
    :dispatch-n [[:fetch-products]
                 [:fetch-orders]]}))

(rf/reg-event-db
 :set-active-panel
 (fn [db [_ panel-id]]
   (assoc db :active-panel panel-id)))

(rf/reg-event-db
 :clear-notification
 (fn [db _]
   (assoc db :notification nil)))

(rf/reg-event-db
 :notify
 (fn [db [_ type message]]
   (assoc db :notification {:type type :message message})))

(rf/reg-event-db
 :set-category-filter
 (fn [db [_ category]]
   (assoc db :category-filter category)))

(rf/reg-event-fx
 :fetch-products
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :get
                 :uri "/api/products"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:fetch-products-success]
                 :on-failure [:api-error "Failed to load products"]}}))

(rf/reg-event-db
 :fetch-products-success
 (fn [db [_ products]]
   (assoc db
          :loading? false
          :products (vec products))))

(rf/reg-event-fx
 :search-products
 (fn [{:keys [db]} [_ term]]
   (let [cleaned (str/trim (or term ""))]
     {:db (assoc db :search-term term :loading? true)
      :http-xhrio {:method :get
                   :uri (if (str/blank? cleaned)
                          "/api/products"
                          (str "/api/products/search?q=" (js/encodeURIComponent cleaned)))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:fetch-products-success]
                   :on-failure [:api-error "Product search failed"]}})))

(rf/reg-event-db
 :open-create-product-modal
 (fn [db _]
   (assoc db :product-modal {:open? true
                             :mode :create
                             :form empty-product-form
                             :error nil})))

(rf/reg-event-db
 :open-edit-product-modal
 (fn [db [_ product]]
   (assoc db :product-modal {:open? true
                             :mode :edit
                             :form {:id (:id product)
                                    :sku (str (:sku product))
                                    :name (str (:name product))
                                    :description (str (or (:description product) ""))
                                    :category (str (:category product))
                                    :price (str (:price product))
                                    :stock (str (:stock product))
                                    :weight-kg (str (:weight-kg product))}
                             :error nil})))

(rf/reg-event-db
 :close-product-modal
 (fn [db _]
   (assoc-in db [:product-modal :open?] false)))

(rf/reg-event-db
 :update-product-form
 (fn [db [_ field value]]
   (-> db
       (assoc-in [:product-modal :form field] value)
       (assoc-in [:product-modal :error] nil))))

(rf/reg-event-fx
 :save-product
 (fn [{:keys [db]} _]
   (let [{:keys [mode form]} (:product-modal db)
         payload {:sku (str/trim (str (:sku form)))
                  :name (str/trim (str (:name form)))
                  :description (str/trim (str (:description form)))
                  :category (str/trim (str (:category form)))
                  :price (js/parseFloat (str (:price form)))
                  :stock (js/parseInt (str (:stock form)) 10)
                  :weight-kg (js/parseFloat (str (:weight-kg form)))}]
     (cond
       (or (str/blank? (:sku payload))
           (str/blank? (:name payload))
           (str/blank? (:category payload)))
       {:db (assoc-in db [:product-modal :error] "SKU, Name, and Category are required.")}

       (or (js/isNaN (:price payload)) (< (:price payload) 0))
       {:db (assoc-in db [:product-modal :error] "Price must be a number greater than or equal to 0.")}

       (or (js/isNaN (:stock payload)) (< (:stock payload) 0))
       {:db (assoc-in db [:product-modal :error] "Stock must be an integer greater than or equal to 0.")}

       (or (js/isNaN (:weight-kg payload)) (< (:weight-kg payload) 0))
       {:db (assoc-in db [:product-modal :error] "Weight (kg) must be a number greater than or equal to 0.")}

       :else
       {:db (assoc db :loading? true)
        :http-xhrio {:method (if (= mode :create) :post :put)
                     :uri (if (= mode :create)
                            "/api/products"
                            (str "/api/products/" (:id form)))
                     :params payload
                     :format (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [:save-product-success mode]
                     :on-failure [:save-product-failure]}}))))

(rf/reg-event-fx
 :save-product-success
 (fn [{:keys [db]} [_ mode _response]]
   {:db (-> db
            (assoc :loading? false)
            (assoc-in [:product-modal :open?] false)
            (assoc :notification {:type :success
                                  :message (if (= mode :create)
                                             "Product created successfully."
                                             "Product updated successfully.")}))
    :dispatch [:search-products (:search-term db)]}))

(rf/reg-event-db
 :save-product-failure
 (fn [db [_ error]]
   (let [msg (or (get-in error [:response :error])
                 "Failed to save product. Please verify the input data.")]
     (-> db
         (assoc :loading? false)
         (assoc-in [:product-modal :error] msg)))))

(rf/reg-event-fx
 :delete-product
 (fn [{:keys [db]} [_ product-id]]
   {:db (-> db
            (assoc :loading? true)
            (update :cart dissoc product-id))
    :http-xhrio {:method :delete
                 :uri (str "/api/products/" product-id)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:delete-product-success]
                 :on-failure [:api-error "Failed to delete product"]}}))

(rf/reg-event-fx
 :delete-product-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc :loading? false)
            (assoc :notification {:type :success
                                  :message "Product deleted from catalog."}))
    :dispatch [:search-products (:search-term db)]}))

(rf/reg-event-db
 :add-to-cart
 (fn [db [_ product]]
   (let [pid (:id product)
         max-stock (:stock product)
         current-qty (get-in db [:cart pid :quantity] 0)
         next-qty (min max-stock (inc current-qty))]
     (if (pos? max-stock)
       (-> db
           (assoc-in [:cart pid] {:product product
                                  :quantity next-qty})
           (assoc :notification {:type :info
                                 :message (str (:name product) " added to cart (" next-qty " in cart).")}))
       db))))

(rf/reg-event-db
 :update-cart-quantity
 (fn [db [_ product-id quantity]]
   (if (<= quantity 0)
     (update db :cart dissoc product-id)
     (if-let [entry (get-in db [:cart product-id])]
       (let [max-stock (get-in entry [:product :stock] 1)
             clamped (min max-stock quantity)]
         (assoc-in db [:cart product-id :quantity] clamped))
       db))))

(rf/reg-event-db
 :remove-from-cart
 (fn [db [_ product-id]]
   (update db :cart dissoc product-id)))

(rf/reg-event-db
 :clear-cart
 (fn [db _]
   (assoc db :cart {})))

(rf/reg-event-fx
 :checkout-cart
 (fn [{:keys [db]} _]
   (let [items (->> (vals (:cart db))
                    (mapv (fn [{:keys [product quantity]}]
                            {:product-id (:id product)
                             :quantity quantity})))]
     (if (empty? items)
       {:db (assoc db :notification {:type :error :message "Your cart is empty."})}
       {:db (assoc db :loading? true)
        :http-xhrio {:method :post
                     :uri "/api/orders"
                     :params {:items items}
                     :format (ajax/json-request-format)
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [:checkout-success]
                     :on-failure [:checkout-failure]}}))))

(rf/reg-event-fx
 :quick-buy
 (fn [{:keys [db]} [_ product]]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :post
                 :uri "/api/orders"
                 :params {:items [{:product-id (:id product)
                                   :quantity 1}]}
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:checkout-success]
                 :on-failure [:checkout-failure]}}))

(rf/reg-event-fx
 :checkout-success
 (fn [{:keys [db]} [_ order]]
   {:db (-> db
            (assoc :loading? false
                   :cart {}
                   :last-order order
                   :active-panel :orders)
            (assoc :notification {:type :success
                                  :message (str "Order completed successfully! Order #" (subs (str (:id order)) 0 8))}))
    :dispatch-n [[:search-products (:search-term db)]
                 [:fetch-orders]]}))

(rf/reg-event-fx
 :checkout-failure
 (fn [{:keys [db]} [_ error]]
   (let [msg (or (get-in error [:response :error])
                 "Could not complete order.")]
     {:db (-> db
              (assoc :loading? false)
              (assoc :notification {:type :error :message msg}))
      :dispatch [:search-products (:search-term db)]})))

(rf/reg-event-fx
 :fetch-orders
 (fn [_ _]
   {:http-xhrio {:method :get
                 :uri "/api/orders"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:fetch-orders-success]
                 :on-failure [:api-error "Failed to fetch orders"]}}))

(rf/reg-event-db
 :fetch-orders-success
 (fn [db [_ orders]]
   (assoc db :orders (vec orders))))

(rf/reg-event-fx
 :upload-csv-file
 (fn [{:keys [db]} [_ js-file]]
   (let [form-data (js/FormData.)]
     (.append form-data "file" js-file)
     {:db (assoc db :importing? true :import-status nil)
      :http-xhrio {:method :post
                   :uri "/api/products/import"
                   :body form-data
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:csv-import-success]
                   :on-failure [:csv-import-failure]}})))

(rf/reg-event-fx
 :import-sample-csv
 (fn [{:keys [db]} _]
   {:db (assoc db :importing? true :import-status nil)
    :http-xhrio {:method :post
                 :uri "/api/products/import-sample"
                 :body (js/FormData.)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:csv-import-success]
                 :on-failure [:csv-import-failure]}}))

(rf/reg-event-fx
 :csv-import-success
 (fn [{:keys [db]} [_ result]]
   {:db (-> db
            (assoc :importing? false
                   :import-status result)
            (assoc :notification {:type :success
                                  :message (str "CSV Import completed: "
                                                (:imported result) " products persisted, "
                                                (:failed result) " rows rejected.")}))
    :dispatch [:fetch-products]}))

(rf/reg-event-db
 :csv-import-failure
 (fn [db [_ error]]
   (let [msg (or (get-in error [:response :error])
                 "Failed to process CSV file.")]
     (-> db
         (assoc :importing? false)
         (assoc :notification {:type :error :message msg})))))

(rf/reg-event-db
 :api-error
 (fn [db [_ prefix error]]
   (let [detail (or (get-in error [:response :error])
                    (:status-text error)
                    "Network error")]
     (-> db
         (assoc :loading? false)
         (assoc :notification {:type :error
                               :message (str prefix ": " detail)})))))

(rf/reg-fx
 :http-xhrio
 (fn [opts]
   (let [{:keys [method uri params body format response-format on-success on-failure]} opts
         req-map (cond-> {:handler #(rf/dispatch (conj on-success %))
                          :error-handler #(rf/dispatch (conj on-failure %))}
                   response-format (assoc :response-format response-format)
                   format (assoc :format format)
                   params (assoc :params params)
                   body (assoc :body body))]
     (ajax-easy/easy-ajax-request uri (str/upper-case (name method)) req-map))))
