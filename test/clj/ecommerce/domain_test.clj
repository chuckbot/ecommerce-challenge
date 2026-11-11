(ns ecommerce.domain-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ecommerce.domain.csv-import :as csv-import]
            [ecommerce.domain.orders :as domain.orders]
            [ecommerce.schema :as schema])
  (:import (java.util UUID)))

(deftest product-input-validation-test
  (testing "Happy path: valid product input with string numbers is coerced and validated"
    (let [res (schema/validate-product-input
               {:sku "  SKU-100  "
                :name "  Mechanical Keyboard  "
                :description "Hot-swappable switches"
                :category "Electronics"
                :price "$129.99"
                :stock "45"
                :weight-kg "1.15"})]
      (is (:valid? res))
      (is (= "SKU-100" (get-in res [:data :sku])))
      (is (= "Mechanical Keyboard" (get-in res [:data :name])))
      (is (= 129.99 (get-in res [:data :price])))
      (is (= 45 (get-in res [:data :stock])))
      (is (= 1.15 (get-in res [:data :weight-kg])))))

  (testing "Edge cases: blank fields, negative values, and XSS tags are rejected"
    (is (false? (:valid? (schema/validate-product-input
                          {:sku ""
                           :name "Valid Name"
                           :description ""
                           :category "Cat"
                           :price 10.0
                           :stock 5
                           :weight-kg 1.0}))))
    (is (false? (:valid? (schema/validate-product-input
                          {:sku "SKU-1"
                           :name "   "
                           :description ""
                           :category "Cat"
                           :price 10.0
                           :stock 5
                           :weight-kg 1.0}))))
    (is (false? (:valid? (schema/validate-product-input
                          {:sku "SKU-1"
                           :name "<script>alert('xss')</script>"
                           :description ""
                           :category "Cat"
                           :price 10.0
                           :stock 5
                           :weight-kg 1.0}))))
    (is (false? (:valid? (schema/validate-product-input
                          {:sku "SKU-1"
                           :name "Item"
                           :description "<img src=x onerror=alert(1)>"
                           :category "Cat"
                           :price 10.0
                           :stock 5
                           :weight-kg 1.0}))))
    (is (false? (:valid? (schema/validate-product-input
                          {:sku "SKU-1"
                           :name "Item"
                           :description "Desc"
                           :category "Cat"
                           :price -0.01
                           :stock 5
                           :weight-kg 1.0}))))
    (is (false? (:valid? (schema/validate-product-input
                          {:sku "SKU-1"
                           :name "Item"
                           :description "Desc"
                           :category "Cat"
                           :price 10.0
                           :stock -5
                           :weight-kg 1.0}))))))

(deftest csv-row-coercion-and-edge-cases-test
  (testing "Happy path: currency symbols, zero price, zero stock, and SQLi strings are valid domain values"
    (let [currency-row (schema/validate-and-coerce-csv-row
                        {:name "Wireless Mouse"
                         :sku "WM-042"
                         :description "Ergonomic"
                         :category "Electronics"
                         :price "$29.99"
                         :stock "75"
                         :weight-kg "0.12"})
          zero-row (schema/validate-and-coerce-csv-row
                    {:name "Mystery Box"
                     :sku "MB-001"
                     :description "Surprise"
                     :category "Gifts"
                     :price "0.00"
                     :stock "0"
                     :weight-kg "0"})
          sqli-row (schema/validate-and-coerce-csv-row
                    {:name "Robert'); DROP TABLE products;--"
                     :sku "SQL-001"
                     :description "SQLi payload"
                     :category "Games"
                     :price "9.99"
                     :stock "50"
                     :weight-kg "0.5"})]
      (is (:valid? currency-row))
      (is (= 29.99 (get-in currency-row [:data :price])))
      (is (:valid? zero-row))
      (is (= 0.0 (get-in zero-row [:data :price])))
      (is (= 0 (get-in zero-row [:data :stock])))
      (is (:valid? sqli-row))))

  (testing "Edge cases: non-numeric price 'free', negative stock '-5', missing weight"
    (let [free-price (schema/validate-and-coerce-csv-row
                      {:name "Yoga Mat"
                       :sku "YM-015"
                       :description "Mat"
                       :category "Sports"
                       :price "free"
                       :stock "200"
                       :weight-kg "1.2"})
          neg-stock (schema/validate-and-coerce-csv-row
                     {:name "Desk Lamp"
                      :sku "DL-007"
                      :description "Lamp"
                      :category "Home"
                      :price "45.50"
                      :stock "-5"
                      :weight-kg "2.1"})
          missing-weight (schema/validate-and-coerce-csv-row
                          {:name "Gaming Keyboard"
                           :sku "GK-088"
                           :description "RGB"
                           :category "Electronics"
                           :price "129.99"
                           :stock "45"
                           :weight-kg ""})]
      (is (false? (:valid? free-price)))
      (is (contains? (:errors free-price) :price))
      (is (false? (:valid? neg-stock)))
      (is (contains? (:errors neg-stock) :stock))
      (is (false? (:valid? missing-weight)))
      (is (contains? (:errors missing-weight) :weight-kg)))))

(deftest csv-import-batch-processing-test
  (testing "Empty CSV and blank lines handling"
    (is (= {:valid [] :invalid [] :skipped 0 :duplicates []}
           (csv-import/process-csv-data [])))
    (let [res (csv-import/process-csv-data
               [["name" "sku" "description" "category" "price" "stock" "weight_kg"]
                ["" "" "" "" "" "" ""]
                ["   " "" "" "" "" "" ""]])]
      (is (= 0 (count (:valid res))))
      (is (= 0 (count (:invalid res))))
      (is (= 2 (:skipped res)))))

  (testing "In-batch duplicate SKUs are deduplicated with last-write-wins"
    (let [res (csv-import/process-csv-data
               [["name" "sku" "description" "category" "price" "stock" "weight_kg"]
                ["Shoes V1" "RS-001" "Old desc" "Footwear" "89.99" "150" "0.35"]
                ["Shoes V2" "RS-001" "New desc" "Footwear" "94.99" "120" "0.35"]])]
      (is (= 1 (count (:valid res))))
      (is (= 2 (:total-valid-rows res)))
      (is (= 1 (count (:duplicates res))))
      (is (= "Shoes V2" (get-in res [:valid 0 :name])))
      (is (= 94.99 (get-in res [:valid 0 :price])))
      (is (= {:sku "RS-001" :first-line 2 :overwritten-by-line 3}
             (first (:duplicates res))))))

  (testing "Official Code Challenge E-Commerce.csv audit metrics match exact dataset anomalies"
    (let [csv-file (io/file "Code Challenge E-Commerce.csv")]
      (is (.exists csv-file))
      (let [res (csv-import/process-file csv-file)
            error-lines (mapv :line (:invalid res))]
        (is (= 85 (count (:valid res))))
        (is (= 88 (:total-valid-rows res)))
        (is (= 3 (count (:duplicates res))))
        (is (= 2 (:skipped res)))
        (is (= 7 (count (:invalid res))))
        (is (= [7 16 20 25 41 50 52] error-lines))))))

(deftest domain-orders-logic-test
  (testing "normalize-items aggregates duplicate product IDs and sorts deterministically to prevent deadlocks"
    (let [u1 (UUID/fromString "bbbbbbbb-0000-0000-0000-000000000002")
          u2 (UUID/fromString "aaaaaaaa-0000-0000-0000-000000000001")
          normalized (domain.orders/normalize-items
                      [{:product-id u1 :quantity 2}
                       {:product-id u2 :quantity 1}
                       {:product-id u1 :quantity 3}
                       {:product-id u2 :quantity 0}])]
      (is (= [{:product-id u2 :quantity 1}
              {:product-id u1 :quantity 5}]
             normalized))))

  (testing "validate-and-build-line-item and calculate-order-totals compute exact BigDecimal amounts"
    (let [pid (UUID/randomUUID)
          product {:id pid :sku "SKU-1" :name "Coffee" :price 18.75M :stock 10 :weight-kg 1.000M}
          line (domain.orders/validate-and-build-line-item product {:product-id pid :quantity 4})
          totals (domain.orders/calculate-order-totals [line])]
      (is (= 75.00M (:line-amount line)))
      (is (= 4.000M (:line-weight line)))
      (is (= 75.00M (:total-amount totals)))
      (is (= 4.000M (:total-weight-kg totals)))
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Insufficient stock"
           (domain.orders/validate-and-build-line-item product {:product-id pid :quantity 11}))))))
