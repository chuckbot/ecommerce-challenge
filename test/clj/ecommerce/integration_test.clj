(ns ecommerce.integration-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [ecommerce.core :as core]
            [ecommerce.db.products :as db.products]
            [ecommerce.http.handlers :as handlers]
            [ecommerce.http.routes :as routes]
            [integrant.core :as ig]
            [next.jdbc :as jdbc]))

(def ^:dynamic *db* nil)

(defn db-fixture [f]
  (let [cfg (select-keys (core/read-config) [:ecommerce.adapters.db/pool])
        sys (ig/init cfg)
        ds (:ecommerce.adapters.db/pool sys)]
    (try
      (jdbc/execute! ds ["DELETE FROM order_items WHERE sku LIKE 'ITEST-%'"])
      (jdbc/execute! ds ["DELETE FROM products WHERE sku LIKE 'ITEST-%'"])
      (binding [*db* ds]
        (f))
      (finally
        (jdbc/execute! ds ["DELETE FROM order_items WHERE sku LIKE 'ITEST-%'"])
        (jdbc/execute! ds ["DELETE FROM products WHERE sku LIKE 'ITEST-%'"])
        (ig/halt! sys)))))

(use-fixtures :each db-fixture)

(deftest product-crud-and-search-integration-test
  (testing "Happy path: Create, Read, Search (FTS + ILIKE), Update, and Delete product"
    (let [create-res (handlers/create-product
                      *db*
                      {:body-params {:sku "ITEST-001"
                                     :name "Titanium Espresso Tamper"
                                     :description "Precision 58.5mm barista coffee tamper"
                                     :category "Kitchen"
                                     :price 49.50
                                     :stock 25
                                     :weight-kg 0.42}})
          created (:body create-res)
          pid (str (:id created))]
      (is (= 201 (:status create-res)))
      (is (uuid? (:id created)))
      (is (= "ITEST-001" (:sku created)))

      (let [fts-res (handlers/search-products *db* {:query-params {"q" "barista tamper"}})
            ilike-res (handlers/search-products *db* {:query-params {"q" "ITEST-00"}})]
        (is (= 200 (:status fts-res)))
        (is (some #(= (:id created) (:id %)) (:body fts-res)))
        (is (= 200 (:status ilike-res)))
        (is (some #(= (:id created) (:id %)) (:body ilike-res))))

      (let [update-res (handlers/update-product
                        *db*
                        {:path-params {:id pid}
                         :body-params {:sku "ITEST-001"
                                       :name "Titanium Espresso Tamper Pro"
                                       :description "Updated precision tamper"
                                       :category "Kitchen"
                                       :price 54.00
                                       :stock 30
                                       :weight-kg 0.45}})]
        (is (= 200 (:status update-res)))
        (is (= "Titanium Espresso Tamper Pro" (get-in update-res [:body :name])))
        (is (= 30 (get-in update-res [:body :stock]))))

      (let [del-res (handlers/delete-product *db* {:path-params {:id pid}})]
        (is (= 200 (:status del-res)))
        (is (nil? (db.products/get-by-id *db* (:id created)))))))

  (testing "Edge cases: duplicate SKU 409, invalid UUID 400, non-existent UUID 404, SQLi safety"
    (let [first-res (handlers/create-product
                     *db*
                     {:body-params {:sku "ITEST-DUP"
                                    :name "First Item"
                                    :description "Desc"
                                    :category "Test"
                                    :price 10
                                    :stock 5
                                    :weight-kg 0.2}})
          dup-res (handlers/create-product
                   *db*
                   {:body-params {:sku "ITEST-DUP"
                                  :name "Second Item"
                                  :description "Desc"
                                  :category "Test"
                                  :price 20
                                  :stock 10
                                  :weight-kg 0.3}})
          sqli-res (handlers/create-product
                    *db*
                    {:body-params {:sku "ITEST-SQLI"
                                   :name "Robert'); DROP TABLE products;--"
                                   :description "'; DELETE FROM orders; --"
                                   :category "Security"
                                   :price 15.0
                                   :stock 10
                                   :weight-kg 0.5}})
          bad-uuid-update (handlers/update-product
                           *db*
                           {:path-params {:id "not-a-uuid"}
                            :body-params {:sku "ITEST-X"
                                          :name "X"
                                          :category "Test"
                                          :price 1
                                          :stock 1
                                          :weight-kg 1}})
          missing-uuid-delete (handlers/delete-product
                               *db*
                               {:path-params {:id "00000000-0000-0000-0000-000000000000"}})]
      (is (= 201 (:status first-res)))
      (is (= 409 (:status dup-res)))
      (is (= 201 (:status sqli-res)))
      (is (= "Robert'); DROP TABLE products;--" (get-in sqli-res [:body :name])))
      (is (= 400 (:status bad-uuid-update)))
      (is (= 404 (:status missing-uuid-delete))))))

(deftest atomic-purchase-and-concurrency-test
  (testing "Happy path: multi-item order calculates exact totals and decrements inventory"
    (let [p1 (:body (handlers/create-product
                     *db*
                     {:body-params {:sku "ITEST-ORD-1"
                                    :name "Item One"
                                    :description "First"
                                    :category "Test"
                                    :price 19.99
                                    :stock 10
                                    :weight-kg 0.5}}))
          p2 (:body (handlers/create-product
                     *db*
                     {:body-params {:sku "ITEST-ORD-2"
                                    :name "Item Two"
                                    :description "Second"
                                    :category "Test"
                                    :price 5.00
                                    :stock 4
                                    :weight-kg 1.25}}))
          order-res (handlers/create-order
                     *db*
                     {:body-params {:items [{:product-id (str (:id p1)) :quantity 2}
                                            {:product-id (str (:id p2)) :quantity 3}]}})]
      (is (= 201 (:status order-res)))
      (is (= "COMPLETED" (get-in order-res [:body :payment-status])))
      (is (= 54.98M (bigdec (get-in order-res [:body :total-amount]))))
      (is (= 4.750M (bigdec (get-in order-res [:body :total-weight-kg]))))
      (is (= 8 (:stock (db.products/get-by-id *db* (:id p1)))))
      (is (= 1 (:stock (db.products/get-by-id *db* (:id p2)))))))

  (testing "Edge cases: insufficient stock rolls back entire multi-item order transaction"
    (let [p-ok (:body (handlers/create-product
                       *db*
                       {:body-params {:sku "ITEST-RB-1"
                                      :name "Abundant Item"
                                      :description "Has stock"
                                      :category "Test"
                                      :price 10.00
                                      :stock 20
                                      :weight-kg 1.0}}))
          p-low (:body (handlers/create-product
                        *db*
                        {:body-params {:sku "ITEST-RB-2"
                                       :name "Scarce Item"
                                       :description "Only 2 left"
                                       :category "Test"
                                       :price 50.00
                                       :stock 2
                                       :weight-kg 2.0}}))
          failed-order (handlers/create-order
                        *db*
                        {:body-params {:items [{:product-id (str (:id p-ok)) :quantity 5}
                                               {:product-id (str (:id p-low)) :quantity 3}]}})
          empty-order (handlers/create-order *db* {:body-params {:items []}})]
      (is (= 409 (:status failed-order)))
      (is (= 20 (:stock (db.products/get-by-id *db* (:id p-ok)))))
      (is (= 2 (:stock (db.products/get-by-id *db* (:id p-low)))))
      (is (= 400 (:status empty-order)))))

  (testing "Concurrency edge case: 10 parallel threads buying 1 unit each of a 3-stock item"
    (let [limited (:body (handlers/create-product
                          *db*
                          {:body-params {:sku "ITEST-CONC-1"
                                         :name "Limited Edition GPU"
                                         :description "Only 3 units available"
                                         :category "Hardware"
                                         :price 999.00
                                         :stock 3
                                         :weight-kg 1.5}}))
          pid (str (:id limited))
          start-gate (promise)
          workers (doall
                   (repeatedly
                    10
                    #(future
                       @start-gate
                       (handlers/create-order
                        *db*
                        {:body-params {:items [{:product-id pid :quantity 1}]}}))))
          _ (deliver start-gate true)
          results (mapv deref workers)
          statuses (frequencies (map :status results))
          final-product (db.products/get-by-id *db* (:id limited))]
      (is (= 3 (get statuses 201 0)))
      (is (= 7 (get statuses 409 0)))
      (is (= 0 (:stock final-product))))))

(deftest reitit-router-and-http-dispatch-test
  (testing "Reitit router compiles without path conflicts and serves SPA index and API routes"
    (let [app (routes/routes *db*)
          root-res (app {:request-method :get :uri "/"})
          products-res (app {:request-method :get :uri "/api/products"})
          orders-res (app {:request-method :get :uri "/api/orders"})]
      (is (= 200 (:status root-res)))
      (is (= 200 (:status products-res)))
      (is (= 200 (:status orders-res))))))
