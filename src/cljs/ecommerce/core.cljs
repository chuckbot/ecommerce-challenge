(ns ecommerce.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [ecommerce.events]
            [ecommerce.subs]
            [ecommerce.views.core :as views]))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/render [views/main-panel] root-el)))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))