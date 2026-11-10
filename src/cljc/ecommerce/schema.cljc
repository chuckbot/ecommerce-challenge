(ns ecommerce.schema
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [malli.transform :as mt]))

(defn non-blank? [s]
  (and (string? s) (not (str/blank? s))))

(defn no-html-tags? [s]
  (and (string? s)
       (not (re-find #"(?i)<\s*/?\s*(script|iframe|object|embed|svg|img|style|link|meta)\b|<[^>]+on\w+\s*=" s))))

(def NonBlankString
  [:and
   {:error/message "Must not be blank"}
   :string
   [:fn {:error/message "Must not be empty or whitespace"} non-blank?]
   [:fn {:error/message "Must not contain executable HTML or script tags"} no-html-tags?]])

(def OptionalSafeString
  [:and
   :string
   [:fn {:error/message "Must not contain executable HTML or script tags"} no-html-tags?]])

(def NonNegativeNumber
  [:and
   {:error/message "Must be a valid non-negative number"}
   [:or :double :int number?]
   [:>= {:error/message "Must be greater than or equal to 0"} 0]])

(def NonNegativeInt
  [:and
   {:error/message "Must be a valid non-negative integer"}
   :int
   [:>= {:error/message "Must be 0 or greater"} 0]])

(def Product
  [:map
   [:id {:optional true} :uuid]
   [:sku NonBlankString]
   [:name NonBlankString]
   [:description {:optional true} [:maybe OptionalSafeString]]
   [:category NonBlankString]
   [:price NonNegativeNumber]
   [:stock NonNegativeInt]
   [:weight-kg NonNegativeNumber]
   [:created-at {:optional true} :any]
   [:updated-at {:optional true} :any]])

(def ProductInput
  [:map
   [:sku NonBlankString]
   [:name NonBlankString]
   [:description {:optional true} [:maybe OptionalSafeString]]
   [:category NonBlankString]
   [:price NonNegativeNumber]
   [:stock NonNegativeInt]
   [:weight-kg NonNegativeNumber]])

(def OrderItem
  [:map
   [:product-id :uuid]
   [:quantity [:and :int [:> {:error/message "Quantity must be greater than 0"} 0]]]])

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
   [:description OptionalSafeString]
   [:category NonBlankString]
   [:price [:and
            {:error/message "Price must be a valid non-negative decimal"}
            :double
            [:>= {:error/message "Price must be 0 or greater"} 0]]]
   [:stock [:and
            {:error/message "Stock must be a valid non-negative integer"}
            :int
            [:>= {:error/message "Stock cannot be negative"} 0]]]
   [:weight-kg [:and
                {:error/message "Weight (kg) must be a valid non-negative decimal"}
                :double
                [:>= {:error/message "Weight cannot be negative"} 0]]]])

(defn parse-clean-double [x]
  (cond
    (number? x) (double x)
    (string? x) (let [trimmed (-> x str/trim (str/replace #"^\$\s*" ""))]
                  (if (re-matches #"^-?\d+(\.\d+)?$" trimmed)
                    #?(:clj (Double/parseDouble trimmed)
                       :cljs (let [n (js/parseFloat trimmed)]
                               (if (js/isNaN n) x n)))
                    x))
    :else x))

(defn parse-clean-int [x]
  (cond
    (int? x) x
    (string? x) (let [trimmed (str/trim x)]
                  (if (re-matches #"^-?\d+$" trimmed)
                    #?(:clj (Long/parseLong trimmed)
                       :cljs (let [n (js/parseInt trimmed 10)]
                               (if (js/isNaN n) x n)))
                    x))
    :else x))

(def csv-transformer
  (mt/transformer
   (mt/strip-extra-keys-transformer)
   {:name :csv-coercion
    :decoders {:string {:compile (fn [_ _]
                                   (fn [x]
                                     (if (string? x)
                                       (str/trim x)
                                       (str x))))}
               :double {:compile (fn [_ _] parse-clean-double)}
               :int {:compile (fn [_ _] parse-clean-int)}}}))

(defn validate-and-coerce-csv-row [row-map]
  (let [coerced (m/decode CsvRow row-map csv-transformer)]
    (if (m/validate CsvRow coerced)
      {:valid? true :data coerced}
      {:valid? false
       :errors (me/humanize (m/explain CsvRow coerced))
       :raw row-map})))

(defn validate-product-input [data]
  (let [coerced (m/decode ProductInput data csv-transformer)]
    (if (m/validate ProductInput coerced)
      {:valid? true :data coerced}
      {:valid? false
       :errors (me/humanize (m/explain ProductInput coerced))})))
