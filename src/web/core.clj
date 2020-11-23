(ns web.core
  (:require [org.httpkit.server :refer [run-server]]
            [route-map.core]
            [clojure.string :as str]
            [ring.util.codec]
            [ring.util.response]
            [ring.middleware.head]))

(defonce server (atom nil))

(defn index [req]
  {:status 302
   :headers {"location" "static/index.html"}})

(def routes
  {:GET index})

(defn handle-static [{meth :request-method uri :uri :as req}]
  (when (and (#{:get :head} meth)
             (or (str/starts-with? (or uri "") "/static/")
                 (str/starts-with? (or uri "") "/favicon.ico")))
    (let [opts {:root "public"
                :index-files? true
                :allow-symlinks? true}
          path (subs (ring.util.codec/url-decode (:uri req)) 8)]
      (-> (ring.util.response/resource-response path opts)
          (ring.middleware.head/head-response req)))))


(defn handler [{meth :request-method uri :uri :as req}]
  (or (handle-static req)
      (if-let [fn-handler (route-map.core/match [meth uri] routes)]
        ((:match fn-handler) req)
        {:status 404 :body "Not found"}
        ))
  )

;(handler {:request-method :get :uri "/static/js/main.js"})

(defn start []
  (reset! server (run-server #'handler {:port 8080}))
  :started
  )

(defn stop []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)
    :stopped))

(defn restart []
  (stop)
  (start)
  )

(comment
  (restart)
  )

