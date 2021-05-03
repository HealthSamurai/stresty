(ns stresty.world
  (:require [stresty.server.http]
            [stresty.server.core :as stresty]
            [zen.core :as zen]))


(defonce test-server (atom nil))

(defmethod stresty.server.http/rest-op
  'stresty.test-srv/echo-op
  [ztx op req]
  {:status 200 :body (select-keys req [:request-method :uri :query-string :resource])})

(defmethod stresty.server.http/rest-op
  'stresty.test-srv/patch-op
  [ztx op req]
  {:status 200 :body (:resource req)})

(defn stop-test-server []
  (when-let [tsrv @test-server]
    (stresty.server.http/stop-server tsrv)
    (reset! test-server nil)))

(defn start-test-server []
  (stop-test-server)
  (let [srv (zen/new-context {:routes {:routes {"echo" {:get 'stresty.test-srv/echo-op}
                                                "do-patch" {:patch 'stresty.test-srv/patch-op}}}})]
    (zen/read-ns srv 'stresty.test-srv)
    (stresty.server.http/start-server srv {:port 7777})
    (reset! test-server srv)))

(defn main [& args]
  (#'stresty/main args))

(comment
  (stop-test-server)

  )
