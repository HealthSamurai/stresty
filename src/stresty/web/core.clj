(ns stresty.web.core
  (:require [org.httpkit.server :refer [run-server]]
            [route-map.core]
            [clojure.string :as str]
            [ring.util.codec]
            [zen.core :as zen]
            [ring.util.response]
            [ring.middleware.head]
            [edamame.core]
            [clojure.java.io :as io]
            [zen.core :as zen]
            [stresty.runner.clj-http.step :as step-runner]))

(defn index [_ req]
  {:status 302
   :headers {"location" "static/index.html"}})

(defn get-symbol [{ztx :ztx :as ctx} req]
  (let [{:keys [ns name]} (:params req)
        o (zen/get-symbol ztx (symbol ns name))]
    (if (nil? o)
      {:status 404}
      {:status 200
       :body o})))

(defn get-scenarios [{ztx :ztx :as ctx} req]
  {:status 200
   :body (->> (zen/get-tag ztx 'stresty/case)
              (map (fn [nm] (zen/get-symbol ztx nm)))
              vec)})

(defn get-tag [_ _] (throw (ex-info "Not implemented" {})))

(defn run-step [{ztx :ztx :as ctx} req]
  (if-let [body (:body req)]
    (let [resp (first (:results (step-runner/run-step ctx body)))
          _ (prn resp)]
      (prn resp)
      {:status 200
       :body resp})
    {:status 400}
    ))

(def routes
  {:GET index
   "scenarios" {:GET get-scenarios}
   "zen" {"symbol" {[:ns] {[:name] {:GET get-symbol}}}
          "tag" {[:ns] {[:name] {:GET get-tag}}}}
   "run-step" {:POST run-step}})

(defn wrap-static [h]
  (fn [{meth :request-method uri :uri :as req}]
    (if (and (#{:get :head} meth)
             (or (str/starts-with? (or uri "") "/static/")
                 (str/starts-with? (or uri "") "/favicon.ico")))
      (let [opts {:root "public"
                  :index-files? true
                  :allow-symlinks? true}
            path (subs (ring.util.codec/url-decode (:uri req)) 8)]
        (-> (ring.util.response/resource-response path opts)
            (ring.middleware.head/head-response req)))
      (h req))))

(defn wrap-content-type [h]
  (fn [req]
    (let [req* (cond-> req
                 (and (-> req
                          (get :body)
                          nil?
                          not)
                      (-> req
                          (get-in [:headers "content-type"])
                            (= "application/edn")))
                 (assoc :body (-> req
                                  (get :body)
                                  (io/reader :encoding "UTF-8")
                                  slurp
                                  read-string)))
          resp (h req*)]
      (if (:body resp)
        (-> resp
            (update :body str)
            (assoc-in [:headers "content-type"] "application/edn"))
        resp))))

(defn dispatch [*ctx {method :request-method uri :uri :as req}]
  (let [match (route-map.core/match [method uri] routes)
        operation (:match match)]
    (operation @*ctx (merge match req))))

(defn start [*ctx]
  (if (nil? (:web @*ctx))
    (let [handler (-> (fn [req] (dispatch *ctx req))
                      wrap-content-type
                      wrap-static)
          srv (run-server handler (merge {:port 8080} (get-in @*ctx [:config :web])))]
      (swap! *ctx assoc :web srv)
      :started)
    :already-started))

(defn stop [*ctx]
  (when-let [srv (:web @*ctx)]
    (srv)
    (swap! *ctx dissoc :web))
  :stopped)

(defn restart [*ctx]
  (stop *ctx)
  (start *ctx))

(comment

  ;; reload ztx
  (do
    (defonce *context (atom {}))
    (def ztx (zen/new-context))
    (zen/read-ns ztx 'user)
    (zen/read-ns ztx 'config)
    (def config (->> (zen/get-tag ztx 'stresty/config)
                     first
                     (zen.core/get-symbol ztx)))
    (swap! *context assoc :ztx ztx)
    (swap! *context assoc :config config))
  
  (restart *context)
  

  (def req
    (-> {:type 'stresty/http-step
         :POST "/Patient"
         :body {:id "new-patient"}}
        str)
    )
  
  (map
   (partial zen/get-symbol ztx)
   (zen/get-tag ztx 'stresty/case))



  (namespace 'my-ns/wow)
  (name 'my-ns/wow)


  )
