(ns stresty.core-test
  (:require [stresty.core :as sut]
            [clojure.test :as t]
            [stresty.world :as world]))

(t/deftest test-core
  (world/start-test-server)

  (sut/-main "--format=debug" "--path=test" "--ns=stresty.testcase" "tests")

  (t/is (= 1 1))

  )
