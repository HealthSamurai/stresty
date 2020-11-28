(ns stresty.runner.clj-http.core-test
  (:require [stresty.runner.clj-http.core :as sut]
            [zen.core :as zen]
            [matcho]
            [clojure.test :refer :all]))

(deftest stresty-runner-clj-http

  (def ztx (zen/new-context))

  (def tc '{ns user
            import #{stresty}

            my-test
            {:zen/tags #{stresty/case}
             :steps [{:type stresty/http-step
                      :GET "/test"
                      :match {:status 200
                              :body {:hello "world"}}}]}})

  (zen/load-ns ztx tc)

  (def cfg {:url "http://localhost:9090"})

  ;; (sut/run-case ztx cfg 'user/my-test)



  (is (= 1 1))



  )
