(ns ecommerce.views.core)(ns ecommerce.views.core
  (:require [re-frame.core :as rf]))

(defn header []
  [:header {:class "bg-slate-900 text-white p-4 shadow-md"}
   [:div {:class "container mx-auto flex justify-between items-center"}
    [:h1 {:class "text-2xl font-bold tracking-tight"} "Enterprise E-Commerce"]
    [:nav
     [:ul {:class "flex space-x-6 font-medium"}
      [:li [:a {:href "#"
                :class "hover:text-blue-400 transition-colors"
                :on-click #(rf/dispatch [:set-active-panel :catalog])} "Catálogo"]]
      [:li [:a {:href "#"
                :class "hover:text-blue-400 transition-colors"
                :on-click #(rf/dispatch [:set-active-panel :import])} "Importar CSV"]]]]]])

(defn catalog-panel []
  [:div {:class "p-6"}
   [:h2 {:class "text-xl font-semibold mb-4 text-slate-800"} "Catálogo de Productos"]
   [:div {:class "text-slate-500"} "Tabla de catálogo y buscador pendiente de implementación..."]])

(defn import-panel []
  [:div {:class "p-6"}
   [:h2 {:class "text-xl font-semibold mb-4 text-slate-800"} "Importación Masiva (CSV)"]
   [:div {:class "text-slate-500"} "Formulario de subida y reporte de validación pendiente..."]])

(defn main-panel []
  (let [active-panel @(rf/subscribe [:active-panel])]
    [:div {:class "min-h-screen bg-slate-50"}
     [header]
     [:main {:class "container mx-auto mt-8 bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden"}
      (case active-panel
        :catalog [catalog-panel]
        :import [import-panel]
        [:div "404 - No encontrado"])]]))