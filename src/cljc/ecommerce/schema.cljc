(ns ecommerce.schema
  (:require [malli.core :as m]
            [malli.transform :as mt]
            [clojure.string :as str]))

(def NonBlankString
  [:and :string [:fn (fn [s] (not (str/blank? s)))]])

(def Product
  [:map
   [:id {:optional true} :uuid]
   [:sku NonBlankString]
   [:name NonBlankString]
   [:description {:optional true} :string]
   [:category NonBlankString]
   [:price [:and number? [:>= 0]]]
   [:stock [:and int? [:>= 0]]]
   [:weight-kg [:and number? [:>= 0]]]
   [:created-at {:optional true} :any]
   [:updated-at {:optional true} :any]])

(def OrderItem
  [:map
   [:product-id :uuid]
   [:quantity [:and int? [:> 0]]]])

(def OrderRequest
  [:map
   [:items [:vector {:min 1} OrderItem]]])

(def OrderResponse
  [:map
   [:id :uuid]
   [:total-amount number?]
   [:total-weight-kg number?]
   [:payment-status :string]
   [:created-at :any]])

(def CsvRow
  [:map
   [:name NonBlankString]
   [:sku NonBlankString]
   [:description :string]
   [:category NonBlankString]
   [:price [:and number? [:>= 0]]]
   [:stock [:and int? [:>= 0]]]
   [:weight_kg [:and number? [:>= 0]]]])

(def csv-transformer
  (mt/transformer
   (mt/string-transformer)
   (mt/strip-extra-keys-transformer)
   {:name :csv-coercion
    :decoders {:string {:compile (fn [_ _]
                                   (fn [x]
                                     (cond
                                       (string? x) (str/trim x)
                                       :else x)))}
               :double {:compile (fn [_ _]
                                   (fn [x]
                                     (try
                                       (if (string? x)
                                         #?(:clj (Double/parseDouble (str/replace x #"[^\d.]" ""))
                                            :cljs (js/parseFloat (str/replace x #"[^\d.]" "")))
                                         (double x))
                                       (catch #?(:clj Exception :cljs js/Error) _ nil))))}}}))

(defn validate-and-coerce-csv-row [row-map]
  (let [coerced (m/decode CsvRow row-map csv-transformer)]
    (if (m/validate CsvRow coerced)
      {:valid? true :data coerced}
      {:valid? false :errors (m/explain CsvRow coerced) :raw row-map})))