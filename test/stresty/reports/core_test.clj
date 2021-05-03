(ns stresty.reports.core-test
  (:require [stresty.reports.core :as sut]
            [cheshire.core]
            [stresty.world :as world]
            [clojure.test :as t]))

(t/deftest test-compact-fmt

  (world/start-test-server)

  (world/main "path:test" "ns:stresty.testcase" "test" "fmt:compact"
              "env:stresty.testcase/env" "report:json" "output:.tmp/report.json")

  (def res (cheshire.core/parse-string (slurp ".tmp/report.json")))
  (t/is (not (nil? res)))
  

  )
