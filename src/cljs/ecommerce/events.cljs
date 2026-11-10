(ns ecommerce.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   {:products []
    :search-term ""
    :import-status nil
    :active-panel :catalog}))

(rf/reg-event-db
 :set-active-panel
 (fn [db [_ panel-id]]
   (assoc db :active-panel panel-id)))