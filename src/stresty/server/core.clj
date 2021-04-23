(ns stresty.server.core
  (:require
   [zen.core :as zen]
   [org.httpkit.server :as server]
   [clojure.string :as str]
   [clojure.walk]

   [stresty.server.formats :as formats]

   [ring.middleware.content-type]
   [ring.middleware.head]
   [ring.util.codec]
   [ring.util.request]
   [ring.util.response]

   [stresty.operations :as ops]))

(defn form-decode [s]
  #_(clojure.walk/keywordize-keys (ring.util.codec/form-decode s))
  (->> (str/split s #"&")
       (reduce (fn [acc pair]
                 (let [[k v] (str/split pair #"=" 2)]
                   (if k
                     (assoc acc (keyword k) v)
                     acc))) {})))

(defn prepare-request [{meth :request-method qs :query-string headers :headers :as req}]
  (let [params (when qs (form-decode qs))
        params (if (string? params) {(keyword params) nil} params)
        method-override (and (= :post meth) (get headers "x-http-method-override"))
        body (formats/parse-body req)]
    (println ::params params)
    (cond-> req
      body (merge body)
      method-override (assoc :request-method (keyword (str/lower-case method-override)))
      params (update :params merge (or params {})))))


(defn preflight
  [{hs :headers}]
  (let [headers (get hs "access-control-request-headers")
        origin (get hs "origin")
        meth  (get hs "access-control-request-method")]
    {:status 200
     :headers {"Access-Control-Allow-Headers" headers
               "Access-Control-Allow-Methods" meth
               "Access-Control-Allow-Origin" origin
               "Access-Control-Allow-Credentials" "true"
               "Access-Control-Expose-Headers" "Location, Transaction-Meta, Content-Location, Category, Content-Type, X-total-count"}}))

(defn allow [resp req]
  (if-let [origin (get-in req [:headers "origin"])]
    (update resp :headers merge
            {"Access-Control-Allow-Origin" origin
             "Access-Control-Allow-Credentials" "true"
             "Access-Control-Expose-Headers" "Location, Content-Location, Category, Content-Type, X-total-count"})
    resp))

(defn mk-handler [ztx dispatch]
  (fn [req]
    (if (= :options (:request-method req))
      (preflight req)
      (let [req (prepare-request req)
            resp (dispatch ztx req)]
        (-> resp
            (formats/format-response req)
            (allow req))))))

(defn handle-static [h {meth :request-method uri :uri :as req}]
  (if (and (#{:get :head} meth)
           (or (str/starts-with? (or uri "") "/static/")
               (str/starts-with? (or uri "") "/favicon.ico")))
    (let [opts {:root "public" :index-files? true :allow-symlinks? true}
          path (subs (ring.util.codec/url-decode (:uri req)) 8)]
      (-> (ring.util.response/resource-response path opts)
          (ring.middleware.head/head-response req)))
    (h req)))

(defn wrap-static [h]
  (fn [req] (handle-static h req)))

(defmulti rest-operation (fn [ztx op request] (:zen/name op)))

(defmethod rest-operation :default
  [ztx op request]
  {:status 500
   :body {:message (str (:zen/name op) " is not implemented.")}})

(defmethod rest-operation 'sty/index-op
  [ztx op request]
  {:status 200
   :body {:message "Welcome to stesty!"}})

(defmethod rest-operation 'sty/rpc-op
  [ztx rest-op {{meth :method :as args} :body}]
  (if-let [rpc-op (and meth (let [meth (symbol meth)] (zen/get-symbol ztx meth)))]
    {:status 200
     :body (ops/call-op ztx rpc-op args)}
    {:status 404
     :body {:message (str "Operation " meth " is not registered")}}))

(defmethod rest-operation 'sty/get-rpc-op
  [ztx rest-op {{meth :method :as params} :params}]
  (println ::params params)
  (if-let [rpc-op (and meth (let [meth (symbol meth)] (zen/get-symbol ztx meth)))]
    {:status 200
     :body (ops/call-op ztx rpc-op {:params (dissoc params :method)})}
    {:status 404
     :body {:message (str "Operation " meth " is not registered. Params: " (pr-str params))}}))

(defn pathify [path]
  (filterv #(not (str/blank? %)) (str/split path #"/")))

(defn- get-params [node]
  (when (map? node)
    (filter (fn [[k v]] (vector? k)) node)))

(defn *match-route [acc node [x & rpth :as pth] params parents wgt]
  (if (nil? node)
    acc
    (if (empty? pth)
      (conj acc {:parents parents :match node :w wgt :params params})
      (let [pnode (and (map? node) (assoc node :params params))
            acc (if-let [branch (get node x)]
                  (*match-route acc branch rpth params (conj parents pnode) (+ wgt 10))
                  acc)]
        (if (keyword? x)
          acc
          (->> (get-params node)
               (reduce (fn [acc [[k] branch]]
                         (*match-route acc branch rpth (assoc params k x) (conj parents pnode) (+ wgt 2)))
                       acc)))))))

(defn match-route
  [meth uri routes]
  (let [path (conj (pathify uri) meth)
        result (*match-route [] routes path {} [] 0)]
    (->> result (sort-by :w) last)))

(defn route [ztx {uri :uri meth :request-method}]
  (let [{routes :routes} (zen/get-symbol ztx 'sty/api)]
    (when-let [{op-name :match params :params} (match-route meth uri routes)]
      {:op (zen/get-symbol ztx op-name)
       :params params})))

(defn dispatch [ztx request]
  (let [{op :op params :params} (route ztx request)]
    (if op
      (do (println ::op op)
          (rest-operation ztx op (assoc request :route-params params)))
      {:status 404
       :body {:message (str "Route " (:request-method request) " " (:uri request) " is not found. Routes " (zen/get-symbol ztx 'sty/api))}})))

(defn start-server [ztx opts]
  (let [port (or (:port opts) 4174)
        handler (-> (mk-handler ztx dispatch)
                    (wrap-static))
        srv (server/run-server handler {:port port})]
    (println ::server-started port)
    (swap! ztx assoc :server srv)))

(defn stop-server [ztx]
  (when-let [srv (get @ztx :server)]
    (srv)
    (swap! ztx dissoc :server)))

(comment
  (def ztx (zen/new-context {}))
  (zen/read-ns ztx 'sty)

  (start-server ztx {})
  (stop-server ztx)

  )
