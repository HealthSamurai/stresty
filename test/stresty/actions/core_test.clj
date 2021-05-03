(ns stresty.actions.core-test
  (:require [stresty.actions.core :as sut]
            [stresty.server.core :as srv]
            [matcho.core :as matcho]
            [stresty.server.http]
            [zen.core :as zen]
            [clojure.test :as t]
            [stresty.world :as world]))


(t/deftest test-actions

  (world/stop-test-server)

  (def env {:base-url "http://localhost:7777"})
  (def ctx {:state {} :case {} :env env})
  (def ztx (srv/start-server {}))


  (matcho/match
   (sut/action ztx ctx {:act 'sty/http})
   {:error {:message #"Wrong action para"}})

  (matcho/match
   (sut/action ztx ctx {:act 'ups})
   {:error {:message #"is not defined"}})

  (matcho/match
   (sut/action ztx ctx
               {:act 'sty/http
                :method :get
                :url "/echo"})
   {:error {:message #"Connection to .* refused"}})

  (world/start-test-server)
  (Thread/sleep 1000)

  (matcho/match
   (sut/action ztx ctx
               {:act 'sty/http
                :method :get
                :url "/unknown"})
   {:result {:status 404}})

  (matcho/match
   (sut/action ztx ctx
               {:act 'sty/http
                :method :get
                :headers {:if-not-match "ups"}
                :url "/echo"})
   {:result {:status 200
             :body {:uri "/echo"}}})

  (matcho/match
   (sut/action ztx ctx
               {:act 'sty/http
                :url "/do-patch"
                :method :patch
                :body {:message "patch ok"}})

   {:result {:status 200
             :body {:message "patch ok"}}})


  )
