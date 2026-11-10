(ns ecommerce.views.core
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn format-currency [val]
  (let [n (js/parseFloat (str (or val 0)))]
    (if (js/isNaN n)
      "$0.00"
      (str "$" (.toFixed n 2)))))

(defn format-weight [val]
  (let [n (js/parseFloat (str (or val 0)))]
    (if (js/isNaN n)
      "0.00 kg"
      (str (.toFixed n 2) " kg"))))

(defn header []
  (let [active-panel @(rf/subscribe [:active-panel])
        cart-summary @(rf/subscribe [:cart-summary])
        orders @(rf/subscribe [:orders])
        nav-btn (fn [panel-id label badge]
                  [:button
                   {:on-click #(rf/dispatch [:set-active-panel panel-id])
                    :class (str "px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 "
                                (if (= active-panel panel-id)
                                  "bg-blue-600 text-white shadow-sm"
                                  "text-slate-300 hover:bg-slate-800 hover:text-white"))}
                   [:span label]
                   (when (and badge (pos? badge))
                     [:span {:class "px-2 py-0.5 text-xs rounded-full bg-amber-400 text-slate-950 font-bold"}
                      badge])])]
    [:header {:class "bg-slate-900 text-white shadow-lg border-b border-slate-800 sticky top-0 z-30"}
     [:div {:class "container mx-auto px-6 py-4 flex flex-wrap justify-between items-center gap-4"}
      [:div {:class "flex items-center gap-3 cursor-pointer"
             :on-click #(rf/dispatch [:set-active-panel :catalog])}
       [:div {:class "w-9 h-9 rounded-lg bg-blue-600 flex items-center justify-center font-bold text-lg shadow"}
        "E"]
       [:div
        [:h1 {:class "text-xl font-bold tracking-tight leading-none"} "Enterprise E-Commerce"]
        [:p {:class "text-xs text-slate-400 mt-1"} "Hexagonal Clojure / ClojureScript Platform"]]]
      [:nav {:class "flex items-center gap-2 flex-wrap"}
       [nav-btn :catalog "Catalog & CRUD" nil]
       [nav-btn :import "CSV Import" nil]
       [nav-btn :cart (str "Cart (" (format-currency (:total-amount cart-summary)) ")") (:count cart-summary)]
       [nav-btn :orders "Orders" (count orders)]]]]))

(defn notification-banner []
  (when-let [{:keys [type message]} @(rf/subscribe [:notification])]
    (let [styles (case type
                   :success "bg-emerald-50 border-emerald-300 text-emerald-900"
                   :error "bg-rose-50 border-rose-300 text-rose-900"
                   "bg-blue-50 border-blue-300 text-blue-900")]
      [:div {:class "container mx-auto px-6 mt-4"}
       [:div {:class (str "p-4 rounded-xl border flex justify-between items-center shadow-sm " styles)}
        [:div {:class "flex items-center gap-3 text-sm font-medium"}
         [:span message]]
        [:button {:on-click #(rf/dispatch [:clear-notification])
                  :class "text-xs font-bold uppercase tracking-wider px-2.5 py-1 rounded hover:bg-black/5"}
         "Dismiss"]]])))

(defn stock-badge [stock]
  (cond
    (<= stock 0)
    [:span {:class "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800"}
     "Out of Stock (0)"]

    (< stock 25)
    [:span {:class "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800"}
     (str "Low (" stock ")")]

    :else
    [:span {:class "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800"}
     (str stock " in stock")]))

(defn product-modal-view []
  (let [{:keys [open? mode form error]} @(rf/subscribe [:product-modal])]
    (when open?
      [:div {:class "fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4"}
       [:div {:class "bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-lg overflow-hidden"}
        [:div {:class "px-6 py-4 bg-slate-900 text-white flex justify-between items-center"}
         [:h3 {:class "text-lg font-bold"}
          (if (= mode :create) "Create New Product" "Edit Product")]
         [:button {:on-click #(rf/dispatch [:close-product-modal])
                   :class "text-slate-400 hover:text-white text-sm font-semibold"}
          "✕"]]
        [:div {:class "p-6 space-y-4"}
         (when error
           [:div {:class "p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-sm font-medium"}
            error])
         [:div {:class "grid grid-cols-1 sm:grid-cols-2 gap-4"}
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "SKU *"]
           [:input {:type "text"
                    :value (:sku form)
                    :placeholder "e.g. RS-001"
                    :on-change #(rf/dispatch [:update-product-form :sku (.. % -target -value)])
                    :class "w-full px-3 py-2 rounded-lg border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Category *"]
           [:input {:type "text"
                    :value (:category form)
                    :placeholder "e.g. Electronics"
                    :on-change #(rf/dispatch [:update-product-form :category (.. % -target -value)])
                    :class "w-full px-3 py-2 rounded-lg border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]]
         [:div
          [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Product Name *"]
          [:input {:type "text"
                   :value (:name form)
                   :placeholder "Commercial product name"
                   :on-change #(rf/dispatch [:update-product-form :name (.. % -target -value)])
                   :class "w-full px-3 py-2 rounded-lg border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
         [:div
          [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Description"]
          [:textarea {:rows 2
                      :value (:description form)
                      :placeholder "Detailed product description"
                      :on-change #(rf/dispatch [:update-product-form :description (.. % -target -value)])
                      :class "w-full px-3 py-2 rounded-lg border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
         [:div {:class "grid grid-cols-3 gap-4"}
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Price (USD) *"]
           [:input {:type "number"
                    :step "0.01"
                    :min "0"
                    :value (:price form)
                    :on-change #(rf/dispatch [:update-product-form :price (.. % -target -value)])
                    :class "w-full px-3 py-2 rounded-lg border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Stock *"]
           [:input {:type "number"
                    :step "1"
                    :min "0"
                    :value (:stock form)
                    :on-change #(rf/dispatch [:update-product-form :stock (.. % -target -value)])
                    :class "w-full px-3 py-2 rounded-lg border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Weight (kg) *"]
           [:input {:type "number"
                    :step "0.01"
                    :min "0"
                    :value (:weight-kg form)
                    :on-change #(rf/dispatch [:update-product-form :weight-kg (.. % -target -value)])
                    :class "w-full px-3 py-2 rounded-lg border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]]]
        [:div {:class "px-6 py-4 bg-slate-50 border-t border-slate-200 flex justify-end gap-3"}
         [:button {:on-click #(rf/dispatch [:close-product-modal])
                   :class "px-4 py-2 rounded-lg border border-slate-300 text-slate-700 text-sm font-medium hover:bg-slate-100"}
          "Cancel"]
         [:button {:on-click #(rf/dispatch [:save-product])
                   :class "px-5 py-2 rounded-lg bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700 shadow-sm"}
          (if (= mode :create) "Create Product" "Save Changes")]]]])))

(defn catalog-panel []
  (let [products @(rf/subscribe [:filtered-products])
        all-products @(rf/subscribe [:products])
        categories @(rf/subscribe [:categories])
        category-filter @(rf/subscribe [:category-filter])
        search-term @(rf/subscribe [:search-term])
        loading? @(rf/subscribe [:loading?])]
    [:div {:class "p-6 space-y-6"}
     [:div {:class "flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5"}
      [:div
       [:h2 {:class "text-2xl font-bold text-slate-900"} "Product Catalog"]
       [:p {:class "text-sm text-slate-500 mt-1"}
        (str "Showing " (count products) " of " (count all-products) " products registered in PostgreSQL")]]
      [:div {:class "flex flex-wrap items-center gap-3"}
       [:button {:on-click #(rf/dispatch [:import-sample-csv])
                 :class "px-4 py-2 rounded-lg border border-slate-300 bg-white hover:bg-slate-50 text-slate-700 text-sm font-medium transition"}
        "Load Sample CSV"]
       [:button {:on-click #(rf/dispatch [:open-create-product-modal])
                 :class "px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold shadow-sm transition"}
        "+ New Product"]]]

     [:div {:class "flex flex-col lg:flex-row gap-4 items-stretch lg:items-center justify-between bg-slate-50 p-4 rounded-xl border border-slate-200"}
      [:div {:class "relative flex-1"}
       [:input {:type "text"
                :value search-term
                :placeholder "Search by name, SKU, category, or description (Full-Text Search + ILIKE)..."
                :on-change #(rf/dispatch [:search-products (.. % -target -value)])
                :class "w-full pl-4 pr-20 py-2.5 bg-white rounded-lg border border-slate-300 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]
       (when-not (str/blank? search-term)
         [:button {:on-click #(rf/dispatch [:search-products ""])
                   :class "absolute right-2.5 top-2 px-2 py-1 text-xs bg-slate-200 hover:bg-slate-300 text-slate-700 rounded font-medium"}
          "Clear"])]
      [:div {:class "flex items-center gap-2 overflow-x-auto pb-1 lg:pb-0"}
       [:span {:class "text-xs font-semibold uppercase text-slate-500 mr-1"} "Category:"]
       [:button {:on-click #(rf/dispatch [:set-category-filter "ALL"])
                 :class (str "px-3 py-1.5 rounded-lg text-xs font-semibold transition "
                             (if (= category-filter "ALL")
                               "bg-slate-900 text-white"
                               "bg-white border border-slate-300 text-slate-700 hover:bg-slate-100"))}
        "All"]
       (for [cat categories]
         ^{:key cat}
         [:button {:on-click #(rf/dispatch [:set-category-filter cat])
                   :class (str "px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition "
                               (if (= category-filter cat)
                                 "bg-blue-600 text-white"
                                 "bg-white border border-slate-300 text-slate-700 hover:bg-slate-100"))}
          cat])]]

     (cond
       loading?
       [:div {:class "py-16 text-center text-slate-500 font-medium"}
        "Loading product catalog..."]

       (empty? products)
       [:div {:class "py-16 text-center bg-slate-50 rounded-xl border border-dashed border-slate-300 space-y-3"}
        [:p {:class "text-slate-600 font-medium"} "No products found matching the current filters."]
        [:div {:class "flex justify-center gap-3"}
         [:button {:on-click #(rf/dispatch [:import-sample-csv])
                   :class "px-4 py-2 rounded-lg bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700"}
          "Import Sample CSV Now"]
         [:button {:on-click #(rf/dispatch [:open-create-product-modal])
                   :class "px-4 py-2 rounded-lg border border-slate-300 bg-white text-slate-700 text-sm font-medium hover:bg-slate-100"}
          "Create Product Manually"]]]

       :else
       [:div {:class "overflow-x-auto rounded-xl border border-slate-200"}
        [:table {:class "w-full text-left border-collapse"}
         [:thead {:class "bg-slate-100 text-slate-600 text-xs uppercase tracking-wider border-b border-slate-200"}
          [:tr
           [:th {:class "py-3.5 px-4 font-semibold"} "SKU"]
           [:th {:class "py-3.5 px-4 font-semibold"} "Product"]
           [:th {:class "py-3.5 px-4 font-semibold"} "Category"]
           [:th {:class "py-3.5 px-4 font-semibold text-right"} "Price"]
           [:th {:class "py-3.5 px-4 font-semibold text-right"} "Weight"]
           [:th {:class "py-3.5 px-4 font-semibold text-center"} "Stock"]
           [:th {:class "py-3.5 px-4 font-semibold text-right"} "Actions (Purchase & CRUD)"]]]
         [:tbody {:class "divide-y divide-slate-200 text-sm"}
          (for [p products]
            ^{:key (:id p)}
            [:tr {:class "hover:bg-slate-50/80 transition-colors"}
             [:td {:class "py-3.5 px-4 font-mono text-xs font-semibold text-slate-700 whitespace-nowrap"}
              (:sku p)]
             [:td {:class "py-3.5 px-4 max-w-md"}
              [:div {:class "font-semibold text-slate-900"} (:name p)]
              (when-not (str/blank? (:description p))
                [:div {:class "text-xs text-slate-500 line-clamp-1 mt-0.5"} (:description p)])]
             [:td {:class "py-3.5 px-4 whitespace-nowrap"}
              [:span {:class "px-2.5 py-1 rounded-md bg-slate-100 text-slate-700 text-xs font-medium"}
               (:category p)]]
             [:td {:class "py-3.5 px-4 text-right font-semibold text-slate-900 whitespace-nowrap"}
              (format-currency (:price p))]
             [:td {:class "py-3.5 px-4 text-right text-slate-600 whitespace-nowrap text-xs"}
              (format-weight (:weight-kg p))]
             [:td {:class "py-3.5 px-4 text-center whitespace-nowrap"}
              [stock-badge (:stock p)]]
             [:td {:class "py-3.5 px-4 text-right whitespace-nowrap"}
              [:div {:class "inline-flex items-center gap-1.5"}
               [:button {:disabled (<= (:stock p) 0)
                         :on-click #(rf/dispatch [:add-to-cart p])
                         :class (str "px-2.5 py-1.5 rounded-lg text-xs font-semibold transition "
                                     (if (pos? (:stock p))
                                       "bg-blue-600 text-white hover:bg-blue-700"
                                       "bg-slate-200 text-slate-400 cursor-not-allowed"))}
                "+ Cart"]
               [:button {:disabled (<= (:stock p) 0)
                         :on-click #(rf/dispatch [:quick-buy p])
                         :class (str "px-2.5 py-1.5 rounded-lg text-xs font-semibold transition "
                                     (if (pos? (:stock p))
                                       "bg-emerald-600 text-white hover:bg-emerald-700"
                                       "bg-slate-200 text-slate-400 cursor-not-allowed"))}
                "Buy 1"]
               [:button {:on-click #(rf/dispatch [:open-edit-product-modal p])
                         :class "px-2.5 py-1.5 rounded-lg border border-slate-300 bg-white hover:bg-slate-100 text-slate-700 text-xs font-medium"}
                "Edit"]
               [:button {:on-click #(rf/dispatch [:delete-product (:id p)])
                         :class "px-2.5 py-1.5 rounded-lg border border-rose-200 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-medium"}
                "Delete"]]]])]]])]))

(defn import-panel []
  (let [drag-over? (r/atom false)]
    (fn []
      (let [import-status @(rf/subscribe [:import-status])
            importing? @(rf/subscribe [:importing?])]
        [:div {:class "p-6 space-y-6"}
         [:div {:class "flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5"}
          [:div
           [:h2 {:class "text-2xl font-bold text-slate-900"} "Bulk Product Import (CSV)"]
           [:p {:class "text-sm text-slate-500 mt-1"}
            "RFC 4180 functional parser with line-by-line Malli validation, currency sanitization, XSS detection, and SKU deduplication."]]
          [:button {:disabled importing?
                    :on-click #(rf/dispatch [:import-sample-csv])
                    :class "px-4 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-sm font-semibold shadow-sm transition"}
           (if importing? "Processing CSV..." "Import Challenge Sample CSV")]]

         [:div
          {:on-drag-over (fn [e]
                           (.preventDefault e)
                           (reset! drag-over? true))
           :on-drag-leave (fn [e]
                            (.preventDefault e)
                            (reset! drag-over? false))
           :on-drop (fn [e]
                      (.preventDefault e)
                      (reset! drag-over? false)
                      (when-let [file (aget (.. e -dataTransfer -files) 0)]
                        (rf/dispatch [:upload-csv-file file])))
           :class (str "border-2 border-dashed rounded-2xl p-10 text-center transition-all "
                       (if @drag-over?
                         "border-blue-500 bg-blue-50/60"
                         "border-slate-300 bg-slate-50 hover:border-slate-400"))}
          [:div {:class "max-w-md mx-auto space-y-4"}
           [:div {:class "w-12 h-12 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center mx-auto font-bold text-xl"}
            "↑"]
           [:div
            [:p {:class "text-base font-semibold text-slate-800"}
             "Drag and drop your .CSV file here"]
            [:p {:class "text-xs text-slate-500 mt-1"}
             "Expected columns: name, sku, description, category, price, stock, weight_kg"]]
           [:div {:class "pt-2"}
            [:label {:class "inline-block px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold cursor-pointer shadow-sm transition"}
             (if importing? "Uploading & Validating..." "Select CSV File")
             [:input {:type "file"
                      :accept ".csv,text/csv"
                      :class "hidden"
                      :on-change (fn [e]
                                   (when-let [file (aget (.. e -target -files) 0)]
                                     (rf/dispatch [:upload-csv-file file])
                                     (set! (.. e -target -value) "")))}]]]]]

         (when import-status
           [:div {:class "space-y-6 pt-2"}
            [:h3 {:class "text-lg font-bold text-slate-900"} "Data Quality Audit Report"]
            [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4"}
             [:div {:class "p-4 rounded-xl bg-emerald-50 border border-emerald-200"}
              [:div {:class "text-xs font-bold uppercase text-emerald-700"} "Products Persisted (Upsert)"]
              [:div {:class "text-3xl font-extrabold text-emerald-900 mt-1"} (:imported import-status)]
              [:div {:class "text-xs text-emerald-700 mt-1"}
               (str "Out of " (:total-valid-rows import-status) " valid CSV rows")]]
             [:div {:class "p-4 rounded-xl bg-blue-50 border border-blue-200"}
              [:div {:class "text-xs font-bold uppercase text-blue-700"} "Duplicate SKUs Merged"]
              [:div {:class "text-3xl font-extrabold text-blue-900 mt-1"} (or (:duplicates-merged import-status) 0)]
              [:div {:class "text-xs text-blue-700 mt-1"} "Last occurrence wins in batch"]]
             [:div {:class "p-4 rounded-xl bg-amber-50 border border-amber-200"}
              [:div {:class "text-xs font-bold uppercase text-amber-700"} "Blank Rows Skipped"]
              [:div {:class "text-3xl font-extrabold text-amber-900 mt-1"} (:skipped import-status)]
              [:div {:class "text-xs text-amber-700 mt-1"} "Empty rows (,,,,,,)"]]
             [:div {:class "p-4 rounded-xl bg-rose-50 border border-rose-200"}
              [:div {:class "text-xs font-bold uppercase text-rose-700"} "Invalid Rows Rejected"]
              [:div {:class "text-3xl font-extrabold text-rose-900 mt-1"} (:failed import-status)]
              [:div {:class "text-xs text-rose-700 mt-1"} "Isolated without aborting batch"]]]

            (when (seq (:duplicates import-status))
              [:div {:class "rounded-xl border border-blue-200 overflow-hidden"}
               [:div {:class "bg-blue-50 px-4 py-3 border-b border-blue-200 font-semibold text-sm text-blue-950"}
                "In-Batch Duplicate SKUs Detected & Merged"]
               [:table {:class "w-full text-left text-sm"}
                [:thead {:class "bg-white text-xs uppercase text-slate-500 border-b border-slate-200"}
                 [:tr
                  [:th {:class "py-2.5 px-4"} "SKU"]
                  [:th {:class "py-2.5 px-4"} "Previous Line"]
                  [:th {:class "py-2.5 px-4"} "Overwritten By Line"]
                  [:th {:class "py-2.5 px-4"} "Resolution"]]]
                [:tbody {:class "divide-y divide-slate-200"}
                 (for [[idx dup] (map-indexed vector (:duplicates import-status))]
                   ^{:key idx}
                   [:tr
                    [:td {:class "py-2.5 px-4 font-mono font-semibold text-blue-800"} (:sku dup)]
                    [:td {:class "py-2.5 px-4"} (str "Line " (:first-line dup))]
                    [:td {:class "py-2.5 px-4 font-semibold"} (str "Line " (:overwritten-by-line dup))]
                    [:td {:class "py-2.5 px-4 text-xs text-slate-600"}
                     "Deduplicated in-memory prior to ON CONFLICT DO UPDATE"]])]]])

            (when (seq (:errors import-status))
              [:div {:class "rounded-xl border border-rose-200 overflow-hidden"}
               [:div {:class "bg-rose-50 px-4 py-3 border-b border-rose-200 font-semibold text-sm text-rose-950"}
                "Rejected Rows — Domain Validation Breakdown (Malli)"]
               [:div {:class "overflow-x-auto"}
                [:table {:class "w-full text-left text-sm"}
                 [:thead {:class "bg-white text-xs uppercase text-slate-500 border-b border-slate-200"}
                  [:tr
                   [:th {:class "py-2.5 px-4"} "CSV Line"]
                   [:th {:class "py-2.5 px-4"} "SKU"]
                   [:th {:class "py-2.5 px-4"} "Raw Name"]
                   [:th {:class "py-2.5 px-4"} "Validation Errors"]
                   [:th {:class "py-2.5 px-4"} "Raw Payload"]]]
                 [:tbody {:class "divide-y divide-slate-200"}
                  (for [err (:errors import-status)]
                    ^{:key (:line err)}
                    [:tr {:class "hover:bg-rose-50/30"}
                     [:td {:class "py-3 px-4 font-bold text-rose-700 whitespace-nowrap"}
                      (str "Line " (:line err))]
                     [:td {:class "py-3 px-4 font-mono text-xs whitespace-nowrap"}
                      (or (:sku err) "—")]
                     [:td {:class "py-3 px-4 font-mono text-xs text-slate-700"}
                      (if (str/blank? (:name err))
                        [:span {:class "italic text-slate-400"} "(empty / whitespace)"]
                        (:name err))]
                     [:td {:class "py-3 px-4 text-xs text-rose-800 font-medium"}
                      (pr-str (:errors err))]
                     [:td {:class "py-3 px-4 font-mono text-xs text-slate-500 max-w-xs truncate"}
                      (pr-str (:raw err))]])]]]])])]))))

(defn cart-panel []
  (let [cart-items @(rf/subscribe [:cart-items])
        summary @(rf/subscribe [:cart-summary])
        loading? @(rf/subscribe [:loading?])]
    [:div {:class "p-6 space-y-6"}
     [:div {:class "flex justify-between items-center border-b border-slate-200 pb-5"}
      [:div
       [:h2 {:class "text-2xl font-bold text-slate-900"} "Shopping Cart & Atomic Checkout"]
       [:p {:class "text-sm text-slate-500 mt-1"}
        "Purchases lock product rows in PostgreSQL (SELECT ... FOR UPDATE) inside an ACID transaction to prevent inventory race conditions."]]
      (when (seq cart-items)
        [:button {:on-click #(rf/dispatch [:clear-cart])
                  :class "px-3 py-1.5 rounded-lg border border-slate-300 text-slate-600 text-xs font-semibold hover:bg-slate-100"}
         "Clear Cart"])]

     (if (empty? cart-items)
       [:div {:class "py-16 text-center bg-slate-50 rounded-xl border border-dashed border-slate-300 space-y-3"}
        [:p {:class "text-slate-600 font-medium"} "Your shopping cart is empty."]
        [:button {:on-click #(rf/dispatch [:set-active-panel :catalog])
                  :class "px-5 py-2.5 rounded-xl bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700"}
         "Browse Catalog"]]
       [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-6"}
        [:div {:class "lg:col-span-2 rounded-xl border border-slate-200 overflow-hidden"}
         [:table {:class "w-full text-left border-collapse text-sm"}
          [:thead {:class "bg-slate-100 text-slate-600 text-xs uppercase border-b border-slate-200"}
           [:tr
            [:th {:class "py-3 px-4"} "Product"]
            [:th {:class "py-3 px-4 text-right"} "Unit Price"]
            [:th {:class "py-3 px-4 text-center"} "Quantity"]
            [:th {:class "py-3 px-4 text-right"} "Subtotal"]
            [:th {:class "py-3 px-4"}]]]
          [:tbody {:class "divide-y divide-slate-200"}
           (for [{:keys [product quantity]} cart-items
                 :let [pid (:id product)
                       price (js/parseFloat (str (:price product)))
                       subtotal (* price quantity)]]
             ^{:key pid}
             [:tr
              [:td {:class "py-3.5 px-4"}
               [:div {:class "font-semibold text-slate-900"} (:name product)]
               [:div {:class "text-xs text-slate-500 font-mono"}
                (str "SKU: " (:sku product) " · Available: " (:stock product))]]
              [:td {:class "py-3.5 px-4 text-right font-medium"}
               (format-currency price)]
              [:td {:class "py-3.5 px-4 text-center"}
               [:div {:class "inline-flex items-center border border-slate-300 rounded-lg overflow-hidden"}
                [:button {:on-click #(rf/dispatch [:update-cart-quantity pid (dec quantity)])
                          :class "px-2.5 py-1 bg-slate-100 hover:bg-slate-200 font-bold"}
                 "-"]
                [:span {:class "px-3 py-1 font-semibold text-sm"} quantity]
                [:button {:on-click #(rf/dispatch [:update-cart-quantity pid (inc quantity)])
                          :class "px-2.5 py-1 bg-slate-100 hover:bg-slate-200 font-bold"}
                 "+"]]]
              [:td {:class "py-3.5 px-4 text-right font-bold text-slate-900"}
               (format-currency subtotal)]
              [:td {:class "py-3.5 px-4 text-right"}
               [:button {:on-click #(rf/dispatch [:remove-from-cart pid])
                         :class "text-rose-600 hover:text-rose-800 text-xs font-semibold"}
                "Remove"]]])]]]

        [:div {:class "p-6 rounded-xl bg-slate-900 text-white space-y-5 h-fit"}
         [:h3 {:class "text-lg font-bold border-b border-slate-800 pb-3"} "Order Summary"]
         [:div {:class "space-y-2 text-sm"}
          [:div {:class "flex justify-between text-slate-300"}
           [:span "Total units:"]
           [:span {:class "font-semibold text-white"} (:count summary)]]
          [:div {:class "flex justify-between text-slate-300"}
           [:span "Estimated total weight:"]
           [:span {:class "font-semibold text-white"} (format-weight (:total-weight summary))]]
          [:div {:class "flex justify-between text-slate-300"}
           [:span "Payment gateway:"]
           [:span {:class "px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 text-xs font-semibold"}
            "Simulated (Instant)"]]
          [:div {:class "flex justify-between text-lg font-bold pt-3 border-t border-slate-800"}
           [:span "Total Due:"]
           [:span {:class "text-emerald-400"} (format-currency (:total-amount summary))]]]
         [:button {:disabled loading?
                   :on-click #(rf/dispatch [:checkout-cart])
                   :class "w-full py-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold text-sm shadow-lg transition"}
          (if loading? "Processing Transaction..." "Confirm & Pay Now")]]])]))

(defn orders-panel []
  (let [orders @(rf/subscribe [:orders])
        last-order @(rf/subscribe [:last-order])]
    [:div {:class "p-6 space-y-6"}
     [:div {:class "flex justify-between items-center border-b border-slate-200 pb-5"}
      [:div
       [:h2 {:class "text-2xl font-bold text-slate-900"} "Order History"]
       [:p {:class "text-sm text-slate-500 mt-1"}
        "Transactional purchase log with atomic inventory deduction."]]
      [:button {:on-click #(rf/dispatch [:fetch-orders])
                :class "px-3.5 py-2 rounded-lg border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50"}
       "Refresh"]]

     (when last-order
       [:div {:class "p-5 rounded-xl bg-emerald-50 border border-emerald-200 space-y-2"}
        [:div {:class "flex justify-between items-center"}
         [:span {:class "text-xs font-bold uppercase tracking-wider text-emerald-800"}
          "Latest Confirmed Purchase Receipt"]
         [:span {:class "px-2.5 py-0.5 rounded-full bg-emerald-200 text-emerald-900 text-xs font-bold"}
          (:payment-status last-order)]]
        [:div {:class "text-sm text-emerald-950 font-semibold"}
         (str "Order ID: " (:id last-order)
              " · Total: " (format-currency (:total-amount last-order))
              " · Weight: " (format-weight (:total-weight-kg last-order)))]])

     (if (empty? orders)
       [:div {:class "py-16 text-center bg-slate-50 rounded-xl border border-dashed border-slate-300 space-y-3"}
        [:p {:class "text-slate-600 font-medium"} "No purchase orders have been placed yet."]
        [:button {:on-click #(rf/dispatch [:set-active-panel :catalog])
                  :class "px-4 py-2 rounded-lg bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700"}
         "Go to Catalog"]]
       [:div {:class "space-y-4"}
        (for [order orders]
          ^{:key (:id order)}
          [:div {:class "rounded-xl border border-slate-200 overflow-hidden bg-white shadow-2xs"}
           [:div {:class "bg-slate-50 px-5 py-3.5 border-b border-slate-200 flex flex-wrap justify-between items-center gap-3"}
            [:div {:class "space-y-0.5"}
             [:div {:class "text-xs font-mono font-semibold text-slate-700"}
              (str "ORDER #" (:id order))]
             [:div {:class "text-xs text-slate-500"}
              (str "Created at: " (:created-at order))]]
            [:div {:class "flex items-center gap-4"}
             [:div {:class "text-right"}
              [:div {:class "text-xs text-slate-500"} "Total Weight"]
              [:div {:class "text-sm font-semibold text-slate-800"} (format-weight (:total-weight-kg order))]]
             [:div {:class "text-right"}
              [:div {:class "text-xs text-slate-500"} "Total Amount"]
              [:div {:class "text-base font-bold text-slate-900"} (format-currency (:total-amount order))]]
             [:span {:class "px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold"}
              (:payment-status order)]]]
           (when (seq (:items order))
             [:div {:class "px-5 py-3 divide-y divide-slate-100 text-sm"}
              (for [item (:items order)]
                ^{:key (:id item)}
                [:div {:class "py-2 flex justify-between items-center"}
                 [:div
                  [:span {:class "font-semibold text-slate-800"} (or (:product-name item) (:sku item))]
                  [:span {:class "ml-2 font-mono text-xs text-slate-500"} (str "(" (:sku item) ")")]]
                 [:div {:class "text-slate-700 font-medium"}
                  (str (:quantity item) " × " (format-currency (:unit-price item)))]])])])])]))

(defn main-panel []
  (let [active-panel @(rf/subscribe [:active-panel])]
    [:div {:class "min-h-screen bg-slate-100 pb-16"}
     [header]
     [notification-banner]
     [product-modal-view]
     [:main {:class "container mx-auto px-6 mt-6"}
      [:div {:class "bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden"}
       (case active-panel
         :catalog [catalog-panel]
         :import [import-panel]
         :cart [cart-panel]
         :orders [orders-panel]
         [catalog-panel])]]]))
