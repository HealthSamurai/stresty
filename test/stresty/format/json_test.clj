(ns stresty.format.json-test
  (:require [stresty.format.json :as sut]
            [stresty.world :as world]
            [clojure.test :as t]))

(t/deftest test-compact-fmt

  (world/start-test-server)

  (world/main "path:test" "ns:stresty.testcase" "test" "fmt:json"
              "env:stresty.testcase/env")

  (t/is (= 1 1))

  ;; (world/main "path:../fhir-stresty" "ns:servers" "test" "fmt:compact")

  )
