(ns ecommerce.domain.csv-import
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ecommerce.schema :as schema]))

(defn normalize-header [headers]
  (mapv #(keyword (str/replace (str/trim (str/lower-case %)) #"_" "-")) headers))

(defn deduplicate-by-sku [valid-entries]
  (let [{:keys [by-sku order duplicates]}
        (reduce (fn [{:keys [by-sku order duplicates]} {:keys [line data]}]
                  (let [sku (:sku data)]
                    (if-let [prev (get by-sku sku)]
                      {:by-sku (assoc by-sku sku {:line line :data data})
                       :order order
                       :duplicates (conj duplicates {:sku sku
                                                     :first-line (:line prev)
                                                     :overwritten-by-line line})}
                      {:by-sku (assoc by-sku sku {:line line :data data})
                       :order (conj order sku)
                       :duplicates duplicates})))
                {:by-sku {} :order [] :duplicates []}
                valid-entries)]
    {:deduplicated (mapv #(get-in by-sku [% :data]) order)
     :duplicates duplicates}))

(defn process-csv-data [csv-data]
  (if (empty? csv-data)
    {:valid [] :invalid [] :skipped 0 :duplicates []}
    (let [[header-row & data-rows] csv-data
          headers (normalize-header header-row)
          raw-result
          (reduce (fn [acc [idx row]]
                    (let [line-num (+ idx 2)]
                      (if (every? str/blank? row)
                        (update acc :skipped inc)
                        (let [padded-row (take (count headers) (concat row (repeat "")))
                              row-map (zipmap headers padded-row)
                              validation (schema/validate-and-coerce-csv-row row-map)]
                          (if (:valid? validation)
                            (update acc :valid-entries conj {:line line-num
                                                             :data (:data validation)})
                            (update acc :invalid conj {:line line-num
                                                       :sku (get row-map :sku "")
                                                       :name (get row-map :name "")
                                                       :errors (:errors validation)
                                                       :raw row-map}))))))
                  {:valid-entries [] :invalid [] :skipped 0}
                  (map-indexed vector data-rows))
          {:keys [deduplicated duplicates]} (deduplicate-by-sku (:valid-entries raw-result))]
      {:valid deduplicated
       :total-valid-rows (count (:valid-entries raw-result))
       :invalid (:invalid raw-result)
       :skipped (:skipped raw-result)
       :duplicates duplicates})))

(defn process-file [file-source]
  (with-open [reader (io/reader file-source)]
    (process-csv-data (doall (csv/read-csv reader)))))
