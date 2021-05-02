(ns stresty.server.core-test
  (:require [stresty.server.core :as sut]
            [stresty.world :as world]
            [clojure.test :as t]))

(t/deftest test-server

  (world/start-test-server)

  ;; (sut/main ["format:debug" "path:test" "ns:stresty.testcase" "test"])

  ;; (sut/main ["test"])
  (sut/main [])

  (t/is (= 1 1))

  (sut/main ["format:stdout" "path:../fhir-stresty" "ns:servers" "test" "env:servers/aidbox"])

  )
