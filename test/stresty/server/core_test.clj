(ns stresty.server.core-test
  (:require [stresty.server.core :as sut]
            [zen.core :as zen]
            [matcho.core :as matcho]
            [clojure.test :as t]))

(t/deftest test-server
  (def ztx (zen/new-context {}))
  (zen/read-ns ztx 'sty)

  (matcho/match
   (sut/route ztx {:uri "/" :request-method :get})
   {:op {:zen/name 'sty/index-op}})

  (matcho/match
   (sut/route ztx {:uri "/ups" :request-method :get})
   nil?)


  (matcho/match
   (sut/route ztx {:uri "/rpc" :request-method :post :body {:method 'sty/echo}})
   {:op {:zen/name 'sty/rpc-op}})

  (matcho/match
   (sut/dispatch ztx
                 {:uri "/rpc"
                  :request-method :post
                  :body {:method 'sty/echo :params {:hello "ok"}}} )
   {:status 200
    :body {:result {:hello "ok"}}})
  )
