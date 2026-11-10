(ns ecommerce.domain.csv-import
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ecommerce.schema :as schema]))

(defn normalize-header [headers]
  (mapv #(keyword (str/replace (str/trim %) #"_" "-")) headers))

(defn process-csv-data [csv-data]
  (let [[header-row & data-rows] csv-data
        headers (normalize-header header-row)]
    (reduce (fn [acc [idx row]]
              (let [line-num (+ idx 2)]
                (if (every? str/blank? row)
                  (update acc :skipped inc)
                  (let [row-map (zipmap headers row)
                        validation (schema/validate-and-coerce-csv-row row-map)]
                    (if (:valid? validation)
                      (update acc :valid conj (:data validation))
                      (update acc :invalid conj {:line line-num
                                                 :errors (:errors validation)
                                                 :raw row-map}))))))
            {:valid [] :invalid [] :skipped 0}
            (map-indexed vector data-rows))))

(defn process-file [file-source]
  (with-open [reader (io/reader file-source)]
    (process-csv-data (csv/read-csv reader))))