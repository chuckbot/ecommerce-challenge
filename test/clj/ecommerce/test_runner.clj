(ns ecommerce.test-runner
  (:require [clojure.test :as t]
            [ecommerce.domain-test]
            [ecommerce.integration-test]))

(defn -main [& _]
  (let [summary (t/run-tests 'ecommerce.domain-test
                             'ecommerce.integration-test)]
    (if (pos? (+ (:fail summary 0) (:error summary 0)))
      (System/exit 1)
      (System/exit 0))))
