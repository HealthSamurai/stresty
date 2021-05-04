(ns stresty.reports.html-test
  (:require [stresty.reports.html :as sut]
            [cheshire.core]
            [stresty.world :as world]
            [clojure.test :as t]))

(t/deftest test-compact-fmt

  (world/start-test-server)

  (world/main "path:test" "ns:stresty.testcase" "test" "fmt:compact"
              "env:stresty.testcase/env" "report:html" "output:.tmp/report.html")

  (def res (slurp ".tmp/report.html"))
  (t/is (not (nil? res)))

  )
