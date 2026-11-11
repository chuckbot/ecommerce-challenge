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

(defn search-icon []
  [:svg {:class "w-5 h-5 text-slate-400 pointer-events-none"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :stroke-width "2"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :d "M21 21l-4.35-4.35m0 0A7.5 7.5 0 104.5 4.5a7.5 7.5 0 0012.15 12.15z"}]])

(defn catalog-icon []
  [:svg {:class "w-5 h-5 shrink-0"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :stroke-width "2"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :d "M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"}]])

(defn import-icon []
  [:svg {:class "w-5 h-5 shrink-0"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :stroke-width "2"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :d "M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"}]])

(defn cart-icon []
  [:svg {:class "w-5 h-5 shrink-0"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :stroke-width "2"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :d "M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"}]])

(defn orders-icon []
  [:svg {:class "w-5 h-5 shrink-0"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :stroke-width "2"}
   [:path {:stroke-linecap "round"
           :stroke-linejoin "round"
           :d "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"}]])

(defn header []
  (let [active-panel @(rf/subscribe [:active-panel])
        cart-summary @(rf/subscribe [:cart-summary])
        orders @(rf/subscribe [:orders])
        nav-btn (fn [panel-id icon-comp label badge]
                  [:button
                   {:on-click #(rf/dispatch [:set-active-panel panel-id])
                    :class (str "px-3.5 py-2 rounded-xl text-sm font-semibold transition-all flex items-center gap-2 "
                                (if (= active-panel panel-id)
                                  "bg-blue-600 text-white shadow-sm"
                                  "text-slate-300 hover:bg-slate-800 hover:text-white"))}
                   [icon-comp]
                   [:span label]
                   (when (and badge (pos? badge))
                     [:span {:class "px-2 py-0.5 text-xs rounded-full bg-amber-400 text-slate-950 font-bold"}
                      badge])])]
    [:header {:class "bg-slate-900 text-white shadow-lg border-b border-slate-800 sticky top-0 z-30"}
     [:div {:class "max-w-7xl mx-auto px-4 sm:px-6 py-3.5 flex justify-between items-center gap-3"}
      [:div {:class "flex items-center gap-3 cursor-pointer"
             :on-click #(rf/dispatch [:set-active-panel :catalog])}
       [:div {:class "w-9 h-9 rounded-xl bg-blue-600 flex items-center justify-center font-bold text-lg shadow shrink-0"}
        "E"]
       [:div
        [:h1 {:class "text-base sm:text-xl font-bold tracking-tight leading-none"} "Enterprise E-Commerce"]
        [:p {:class "text-[11px] sm:text-xs text-slate-400 mt-1"} "Hexagonal Clojure / ClojureScript PWA"]]]
      [:nav {:class "hidden md:flex items-center gap-2"}
       [nav-btn :catalog catalog-icon "Catalog & CRUD" nil]
       [nav-btn :import import-icon "CSV Import" nil]
       [nav-btn :cart cart-icon (str "Cart (" (format-currency (:total-amount cart-summary)) ")") (:count cart-summary)]
       [nav-btn :orders orders-icon "Orders" (count orders)]]
      [:div {:class "flex md:hidden items-center gap-2"}
       [:button {:on-click #(rf/dispatch [:set-active-panel :cart])
                 :class "px-3 py-2 rounded-xl bg-slate-800 text-white text-xs font-semibold flex items-center gap-2 border border-slate-700 shadow-xs"}
        [cart-icon]
        [:span (format-currency (:total-amount cart-summary))]
        [:span {:class "px-1.5 py-0.5 rounded-full bg-amber-400 text-slate-950 font-bold"}
         (:count cart-summary)]]]]]))

(defn mobile-bottom-nav []
  (let [active-panel @(rf/subscribe [:active-panel])
        cart-summary @(rf/subscribe [:cart-summary])
        orders @(rf/subscribe [:orders])
        tab-item (fn [panel-id icon-comp label badge]
                   (let [active? (= active-panel panel-id)]
                     [:button
                      {:on-click #(rf/dispatch [:set-active-panel panel-id])
                       :class (str "py-2.5 px-2 rounded-xl flex flex-col items-center justify-center gap-1 relative transition-all "
                                   (if active?
                                     "bg-blue-600 text-white shadow-md font-bold"
                                     "text-slate-300 hover:bg-slate-800/80 hover:text-white font-medium"))}
                      [:div {:class "relative flex items-center justify-center"}
                       [icon-comp]
                       (when (and badge (pos? badge))
                         [:span {:class (str "absolute -top-2 -right-3.5 min-w-[18px] h-[18px] px-1 rounded-full text-[10px] font-extrabold flex items-center justify-center shadow "
                                             (if active?
                                               "bg-amber-400 text-slate-950"
                                               "bg-blue-500 text-white"))}
                          badge])]
                      [:span {:class "text-[11px] tracking-wide leading-none mt-0.5"}
                       label]]))]
    [:div {:class "md:hidden fixed bottom-3 inset-x-3 z-40 pointer-events-none"}
     [:nav {:class "pointer-events-auto bg-slate-900/95 backdrop-blur-md border border-slate-700 rounded-2xl shadow-2xl p-1.5 grid grid-cols-4 gap-1.5"}
      [tab-item :catalog catalog-icon "Catalog" nil]
      [tab-item :import import-icon "CSV Import" nil]
      [tab-item :cart cart-icon "Cart" (:count cart-summary)]
      [tab-item :orders orders-icon "Orders" (count orders)]]]))

(defn notification-banner []
  (when-let [{:keys [type message]} @(rf/subscribe [:notification])]
    (let [styles (case type
                   :success "bg-emerald-50 border-emerald-300 text-emerald-900"
                   :error "bg-rose-50 border-rose-300 text-rose-900"
                   "bg-blue-50 border-blue-300 text-blue-900")]
      [:div {:class "max-w-7xl mx-auto px-4 sm:px-6 mt-4"}
       [:div {:class (str "p-3.5 sm:p-4 rounded-xl border flex justify-between items-center gap-3 shadow-xs " styles)}
        [:div {:class "text-xs sm:text-sm font-medium"}
         [:span message]]
        [:button {:on-click #(rf/dispatch [:clear-notification])
                  :class "text-xs font-bold uppercase tracking-wider px-2.5 py-1 rounded hover:bg-black/5 shrink-0"}
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
      [:div {:class "fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-end sm:items-center justify-center p-0 sm:p-4"}
       [:div {:class "bg-white rounded-t-2xl sm:rounded-2xl shadow-2xl border border-slate-200 w-full max-w-lg overflow-hidden max-h-[92vh] flex flex-col"}
        [:div {:class "px-5 py-4 bg-slate-900 text-white flex justify-between items-center shrink-0"}
         [:h3 {:class "text-base sm:text-lg font-bold"}
          (if (= mode :create) "Create New Product" "Edit Product")]
         [:button {:on-click #(rf/dispatch [:close-product-modal])
                   :class "text-slate-400 hover:text-white text-sm font-semibold px-2 py-1"}
          "✕"]]
        [:div {:class "p-5 sm:p-6 space-y-4 overflow-y-auto"}
         (when error
           [:div {:class "p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs sm:text-sm font-medium"}
            error])
         [:div {:class "grid grid-cols-1 sm:grid-cols-2 gap-4"}
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "SKU *"]
           [:input {:type "text"
                    :value (:sku form)
                    :placeholder "e.g. RS-001"
                    :on-change #(rf/dispatch [:update-product-form :sku (.. % -target -value)])
                    :class "w-full px-3 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Category *"]
           [:input {:type "text"
                    :value (:category form)
                    :placeholder "e.g. Electronics"
                    :on-change #(rf/dispatch [:update-product-form :category (.. % -target -value)])
                    :class "w-full px-3 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]]
         [:div
          [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Product Name *"]
          [:input {:type "text"
                   :value (:name form)
                   :placeholder "Commercial product name"
                   :on-change #(rf/dispatch [:update-product-form :name (.. % -target -value)])
                   :class "w-full px-3 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
         [:div
          [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Description"]
          [:textarea {:rows 2
                      :value (:description form)
                      :placeholder "Detailed product description"
                      :on-change #(rf/dispatch [:update-product-form :description (.. % -target -value)])
                      :class "w-full px-3 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
         [:div {:class "grid grid-cols-3 gap-3"}
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Price ($) *"]
           [:input {:type "number"
                    :step "0.01"
                    :min "0"
                    :value (:price form)
                    :on-change #(rf/dispatch [:update-product-form :price (.. % -target -value)])
                    :class "w-full px-3 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Stock *"]
           [:input {:type "number"
                    :step "1"
                    :min "0"
                    :value (:stock form)
                    :on-change #(rf/dispatch [:update-product-form :stock (.. % -target -value)])
                    :class "w-full px-3 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]
          [:div
           [:label {:class "block text-xs font-semibold uppercase text-slate-600 mb-1"} "Weight (kg) *"]
           [:input {:type "number"
                    :step "0.01"
                    :min "0"
                    :value (:weight-kg form)
                    :on-change #(rf/dispatch [:update-product-form :weight-kg (.. % -target -value)])
                    :class "w-full px-3 py-2.5 rounded-lg border border-slate-300 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"}]]]]
        [:div {:class "px-5 py-4 bg-slate-50 border-t border-slate-200 flex justify-end gap-3 shrink-0"}
         [:button {:on-click #(rf/dispatch [:close-product-modal])
                   :class "px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-sm font-medium hover:bg-slate-100"}
          "Cancel"]
         [:button {:on-click #(rf/dispatch [:save-product])
                   :class "px-5 py-2.5 rounded-xl bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700 shadow-sm"}
          (if (= mode :create) "Create Product" "Save Changes")]]]])))

(defn pagination-bar [{:keys [total-items total-pages current-page start-item end-item]}]
  (when (pos? total-items)
    [:div {:class "flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-slate-200"}
     [:div {:class "text-xs sm:text-sm text-slate-600"}
      "Showing "
      [:span {:class "font-semibold text-slate-900"} start-item]
      "–"
      [:span {:class "font-semibold text-slate-900"} end-item]
      " of "
      [:span {:class "font-semibold text-slate-900"} total-items]
      " products (15 per page)"]
     (when (> total-pages 1)
       [:div {:class "flex items-center gap-1.5 flex-wrap justify-center"}
        [:button {:disabled (<= current-page 1)
                  :on-click #(rf/dispatch [:set-page (dec current-page)])
                  :class (str "px-3 py-1.5 rounded-lg text-xs font-semibold border transition "
                              (if (<= current-page 1)
                                "border-slate-200 bg-slate-100 text-slate-400 cursor-not-allowed"
                                "border-slate-300 bg-white text-slate-700 hover:bg-slate-50"))}
         "← Prev"]
        (for [page (range 1 (inc total-pages))]
          ^{:key page}
          [:button {:on-click #(rf/dispatch [:set-page page])
                    :class (str "w-8 h-8 rounded-lg text-xs font-bold transition "
                                (if (= page current-page)
                                  "bg-blue-600 text-white shadow-xs"
                                  "border border-slate-300 bg-white text-slate-700 hover:bg-slate-50"))}
           page])
        [:button {:disabled (>= current-page total-pages)
                  :on-click #(rf/dispatch [:set-page (inc current-page)])
                  :class (str "px-3 py-1.5 rounded-lg text-xs font-semibold border transition "
                              (if (>= current-page total-pages)
                                "border-slate-200 bg-slate-100 text-slate-400 cursor-not-allowed"
                                "border-slate-300 bg-white text-slate-700 hover:bg-slate-50"))}
         "Next →"]])]))

(defn mobile-product-card [p]
  [:div {:class "p-4 rounded-xl border border-slate-200 bg-white shadow-2xs space-y-3"}
   [:div {:class "flex items-start justify-between gap-2"}
    [:div
     [:div {:class "flex items-center gap-2 flex-wrap"}
      [:span {:class "font-mono text-xs font-bold px-2 py-0.5 rounded bg-slate-100 text-slate-700"}
       (:sku p)]
      [:span {:class "px-2 py-0.5 rounded-md bg-blue-50 text-blue-700 text-xs font-medium"}
       (:category p)]]
     [:h3 {:class "font-bold text-slate-900 text-base mt-1"} (:name p)]]
    [:div {:class "text-right shrink-0"}
     [:div {:class "text-lg font-extrabold text-slate-900"} (format-currency (:price p))]
     [:div {:class "text-[11px] text-slate-500"} (format-weight (:weight-kg p))]]]
   (when-not (str/blank? (:description p))
     [:p {:class "text-xs text-slate-600 line-clamp-2"} (:description p)])
   [:div {:class "flex items-center justify-between pt-2 border-t border-slate-100"}
    [stock-badge (:stock p)]
    [:div {:class "flex items-center gap-1.5"}
     [:button {:on-click #(rf/dispatch [:open-edit-product-modal p])
               :class "px-2.5 py-1.5 rounded-lg border border-slate-300 bg-white text-slate-700 text-xs font-semibold"}
      "Edit"]
     [:button {:on-click #(rf/dispatch [:delete-product (:id p)])
               :class "px-2.5 py-1.5 rounded-lg border border-rose-200 bg-rose-50 text-rose-700 text-xs font-semibold"}
      "Delete"]]]
   [:div {:class "grid grid-cols-2 gap-2 pt-1"}
    [:button {:disabled (<= (:stock p) 0)
              :on-click #(rf/dispatch [:add-to-cart p])
              :class (str "py-2.5 px-3 rounded-xl text-xs font-bold transition "
                          (if (pos? (:stock p))
                            "bg-blue-600 text-white hover:bg-blue-700"
                            "bg-slate-200 text-slate-400 cursor-not-allowed"))}
     "+ Add to Cart"]
    [:button {:disabled (<= (:stock p) 0)
              :on-click #(rf/dispatch [:quick-buy p])
              :class (str "py-2.5 px-3 rounded-xl text-xs font-bold transition "
                          (if (pos? (:stock p))
                            "bg-emerald-600 text-white hover:bg-emerald-700"
                            "bg-slate-200 text-slate-400 cursor-not-allowed"))}
     "Quick Buy (1)"]]])

(defn catalog-panel []
  (let [pagination @(rf/subscribe [:pagination])
        products (:items pagination)
        all-products @(rf/subscribe [:products])
        categories @(rf/subscribe [:categories])
        category-counts @(rf/subscribe [:category-counts])
        category-filter @(rf/subscribe [:category-filter])
        sort-key @(rf/subscribe [:sort-by])
        search-term @(rf/subscribe [:search-term])
        loading? @(rf/subscribe [:loading?])
        has-active-filters? (or (not (str/blank? search-term))
                                (not= category-filter "ALL")
                                (not= sort-key :recent))]
    [:div {:class "p-4 sm:p-6 space-y-5"}
     [:div {:class "flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-4"}
      [:div
       [:h2 {:class "text-xl sm:text-2xl font-bold text-slate-900"} "Product Catalog"]
       [:p {:class "text-xs sm:text-sm text-slate-500 mt-0.5"}
        (str (:total-items pagination) " matching products · " (count all-products) " total in database")]]
      [:div {:class "grid grid-cols-2 sm:flex items-center gap-2.5"}
       [:button {:on-click #(rf/dispatch [:import-sample-csv])
                 :class "px-3.5 py-2.5 rounded-xl border border-slate-300 bg-white hover:bg-slate-50 text-slate-700 text-xs sm:text-sm font-semibold transition text-center"}
        "Load Sample CSV"]
       [:button {:on-click #(rf/dispatch [:open-create-product-modal])
                 :class "px-4 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs sm:text-sm font-semibold shadow-xs transition text-center"}
        "+ New Product"]]]

     [:div {:class "bg-slate-50 p-3.5 sm:p-4 rounded-2xl border border-slate-200 space-y-3"}
      [:div {:class "grid grid-cols-1 md:grid-cols-12 gap-3"}
       [:div {:class "md:col-span-6 lg:col-span-7"}
        [:label {:class "block text-[11px] font-bold uppercase tracking-wider text-slate-600 mb-1"}
         "Search Products (Name, SKU, Description)"]
        [:div {:class "relative flex items-center"}
         [:div {:class "absolute left-3.5 flex items-center"}
          [search-icon]]
         [:input {:type "search"
                  :value search-term
                  :placeholder "Type to search e.g. 'Running Shoes', 'RS-001', 'wireless'..."
                  :on-change #(rf/dispatch [:search-products (.. % -target -value)])
                  :class "w-full pl-10 pr-20 py-2.5 bg-white text-slate-900 placeholder-slate-400 rounded-xl border border-slate-300 text-sm font-medium shadow-2xs focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"}]
         (when-not (str/blank? search-term)
           [:button {:on-click #(rf/dispatch [:search-products ""])
                     :class "absolute right-2 px-2.5 py-1 text-xs bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-lg font-semibold"}
            "Clear"])]]

       [:div {:class "md:col-span-3 lg:col-span-3"}
        [:label {:class "block text-[11px] font-bold uppercase tracking-wider text-slate-600 mb-1"}
         "Category Filter"]
        [:select {:value category-filter
                  :on-change #(rf/dispatch [:set-category-filter (.. % -target -value)])
                  :class "w-full px-3.5 py-2.5 bg-white text-slate-900 rounded-xl border border-slate-300 text-sm font-medium shadow-2xs focus:outline-none focus:ring-2 focus:ring-blue-500"}
         [:option {:value "ALL"} (str "All Categories (" (count all-products) ")")]
         (for [cat categories]
           ^{:key cat}
           [:option {:value cat}
            (str cat " (" (get category-counts cat 0) ")")])]]

       [:div {:class "md:col-span-3 lg:col-span-2"}
        [:label {:class "block text-[11px] font-bold uppercase tracking-wider text-slate-600 mb-1"}
         "Sort By"]
        [:select {:value (name sort-key)
                  :on-change #(rf/dispatch [:set-sort-by (.. % -target -value)])
                  :class "w-full px-3.5 py-2.5 bg-white text-slate-900 rounded-xl border border-slate-300 text-sm font-medium shadow-2xs focus:outline-none focus:ring-2 focus:ring-blue-500"}
         [:option {:value "recent"} "Recently Updated"]
         [:option {:value "price-asc"} "Price: Low to High"]
         [:option {:value "price-desc"} "Price: High to Low"]
         [:option {:value "stock-asc"} "Stock: Low to High"]
         [:option {:value "stock-desc"} "Stock: High to Low"]
         [:option {:value "name-asc"} "Name: A to Z"]]]]

      (when has-active-filters?
        [:div {:class "flex flex-wrap items-center gap-2 pt-2 border-t border-slate-200/80 text-xs"}
         [:span {:class "font-semibold text-slate-500"} "Active filters:"]
         (when-not (str/blank? search-term)
           [:span {:class "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-blue-100 text-blue-800 font-semibold"}
            (str "Search: \"" search-term "\"")
            [:button {:on-click #(rf/dispatch [:search-products ""])
                      :class "hover:text-blue-950 font-bold"} "✕"]])
         (when (not= category-filter "ALL")
           [:span {:class "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-blue-100 text-blue-800 font-semibold"}
            (str "Category: " category-filter)
            [:button {:on-click #(rf/dispatch [:set-category-filter "ALL"])
                      :class "hover:text-blue-950 font-bold"} "✕"]])
         [:button {:on-click #(rf/dispatch [:reset-catalog-filters])
                   :class "ml-auto text-blue-600 hover:text-blue-800 font-semibold underline"}
          "Reset all"]])]

     (cond
       loading?
       [:div {:class "py-16 text-center text-slate-500 font-medium"}
        "Loading product catalog..."]

       (empty? products)
       [:div {:class "py-14 px-4 text-center bg-slate-50 rounded-2xl border border-dashed border-slate-300 space-y-3"}
        [:p {:class "text-slate-700 font-semibold"} "No products found matching the current filters."]
        [:div {:class "flex flex-wrap justify-center gap-3"}
         (when has-active-filters?
           [:button {:on-click #(rf/dispatch [:reset-catalog-filters])
                     :class "px-4 py-2 rounded-xl border border-slate-300 bg-white text-slate-700 text-sm font-semibold hover:bg-slate-100"}
            "Clear Filters"])
         [:button {:on-click #(rf/dispatch [:import-sample-csv])
                   :class "px-4 py-2 rounded-xl bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700"}
          "Import Sample CSV Now"]
         [:button {:on-click #(rf/dispatch [:open-create-product-modal])
                   :class "px-4 py-2 rounded-xl border border-slate-300 bg-white text-slate-700 text-sm font-medium hover:bg-slate-100"}
          "Create Product Manually"]]]

       :else
       [:div {:class "space-y-4"}
        [:div {:class "grid grid-cols-1 gap-3 md:hidden"}
         (for [p products]
           ^{:key (:id p)}
           [mobile-product-card p])]

        [:div {:class "hidden md:block overflow-x-auto rounded-xl border border-slate-200"}
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
                 "Delete"]]]])]]]

        [pagination-bar pagination]])]))

(defn import-panel []
  (let [drag-over? (r/atom false)]
    (fn []
      (let [import-status @(rf/subscribe [:import-status])
            importing? @(rf/subscribe [:importing?])]
        [:div {:class "p-4 sm:p-6 space-y-6"}
         [:div {:class "flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 pb-5"}
          [:div
           [:h2 {:class "text-xl sm:text-2xl font-bold text-slate-900"} "Bulk Product Import (CSV)"]
           [:p {:class "text-xs sm:text-sm text-slate-500 mt-1"}
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
           :class (str "border-2 border-dashed rounded-2xl p-6 sm:p-10 text-center transition-all "
                       (if @drag-over?
                         "border-blue-500 bg-blue-50/60"
                         "border-slate-300 bg-slate-50 hover:border-slate-400"))}
          [:div {:class "max-w-md mx-auto space-y-4"}
           [:div {:class "w-12 h-12 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center mx-auto font-bold text-xl"}
            "↑"]
           [:div
            [:p {:class "text-sm sm:text-base font-semibold text-slate-800"}
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
               [:div {:class "overflow-x-auto"}
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
                      "Deduplicated in-memory prior to ON CONFLICT DO UPDATE"]])]]]])

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
    [:div {:class "p-4 sm:p-6 space-y-6"}
     [:div {:class "flex justify-between items-center border-b border-slate-200 pb-5"}
      [:div
       [:h2 {:class "text-xl sm:text-2xl font-bold text-slate-900"} "Shopping Cart & Atomic Checkout"]
       [:p {:class "text-xs sm:text-sm text-slate-500 mt-1"}
        "Purchases lock product rows in PostgreSQL (SELECT ... FOR UPDATE) inside an ACID transaction to prevent inventory race conditions."]]
      (when (seq cart-items)
        [:button {:on-click #(rf/dispatch [:clear-cart])
                  :class "px-3 py-1.5 rounded-lg border border-slate-300 text-slate-600 text-xs font-semibold hover:bg-slate-100 shrink-0"}
         "Clear Cart"])]

     (if (empty? cart-items)
       [:div {:class "py-16 text-center bg-slate-50 rounded-xl border border-dashed border-slate-300 space-y-3"}
        [:p {:class "text-slate-600 font-medium"} "Your shopping cart is empty."]
        [:button {:on-click #(rf/dispatch [:set-active-panel :catalog])
                  :class "px-5 py-2.5 rounded-xl bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700"}
         "Browse Catalog"]]
       [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-6"}
        [:div {:class "lg:col-span-2 rounded-xl border border-slate-200 overflow-x-auto"}
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
              [:td {:class "py-3.5 px-4 text-right font-medium whitespace-nowrap"}
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
              [:td {:class "py-3.5 px-4 text-right font-bold text-slate-900 whitespace-nowrap"}
               (format-currency subtotal)]
              [:td {:class "py-3.5 px-4 text-right"}
               [:button {:on-click #(rf/dispatch [:remove-from-cart pid])
                         :class "text-rose-600 hover:text-rose-800 text-xs font-semibold"}
                "Remove"]]])]]]

        [:div {:class "p-5 sm:p-6 rounded-xl bg-slate-900 text-white space-y-5 h-fit"}
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
    [:div {:class "p-4 sm:p-6 space-y-6"}
     [:div {:class "flex justify-between items-center border-b border-slate-200 pb-5"}
      [:div
       [:h2 {:class "text-xl sm:text-2xl font-bold text-slate-900"} "Order History"]
       [:p {:class "text-xs sm:text-sm text-slate-500 mt-1"}
        "Transactional purchase log with atomic inventory deduction."]]
      [:button {:on-click #(rf/dispatch [:fetch-orders])
                :class "px-3.5 py-2 rounded-lg border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50"}
       "Refresh"]]

     (when last-order
       [:div {:class "p-4 sm:p-5 rounded-xl bg-emerald-50 border border-emerald-200 space-y-2"}
        [:div {:class "flex justify-between items-center"}
         [:span {:class "text-xs font-bold uppercase tracking-wider text-emerald-800"}
          "Latest Confirmed Purchase Receipt"]
         [:span {:class "px-2.5 py-0.5 rounded-full bg-emerald-200 text-emerald-900 text-xs font-bold"}
          (:payment-status last-order)]]
        [:div {:class "text-xs sm:text-sm text-emerald-950 font-semibold break-all"}
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
           [:div {:class "bg-slate-50 px-4 sm:px-5 py-3.5 border-b border-slate-200 flex flex-wrap justify-between items-center gap-3"}
            [:div {:class "space-y-0.5"}
             [:div {:class "text-xs font-mono font-semibold text-slate-700 break-all"}
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
             [:div {:class "px-4 sm:px-5 py-3 divide-y divide-slate-100 text-sm"}
              (for [item (:items order)]
                ^{:key (:id item)}
                [:div {:class "py-2 flex justify-between items-center gap-2"}
                 [:div
                  [:span {:class "font-semibold text-slate-800"} (or (:product-name item) (:sku item))]
                  [:span {:class "ml-2 font-mono text-xs text-slate-500"} (str "(" (:sku item) ")")]]
                 [:div {:class "text-slate-700 font-medium whitespace-nowrap"}
                  (str (:quantity item) " × " (format-currency (:unit-price item)))]])])])])]))

(defn main-panel []
  (let [active-panel @(rf/subscribe [:active-panel])]
    [:div {:class "min-h-screen bg-slate-100 pb-28 md:pb-16"}
     [header]
     [notification-banner]
     [product-modal-view]
     [:main {:class "max-w-7xl mx-auto px-3 sm:px-6 mt-4 sm:mt-6"}
      [:div {:class "bg-white rounded-2xl shadow-xs border border-slate-200 overflow-hidden"}
       (case active-panel
         :catalog [catalog-panel]
         :import [import-panel]
         :cart [cart-panel]
         :orders [orders-panel]
         [catalog-panel])]]
     [mobile-bottom-nav]]))
