(ns stresty.server.http
  (:require
   [zen.core :as zen]
   [org.httpkit.server :as server]
   [clojure.string :as str]
   [clojure.walk]

   [stresty.server.formats :as formats]
   [stresty.format.core :as fmt]

   [ring.middleware.content-type]
   [ring.middleware.head]
   [ring.util.codec]
   [ring.util.request]
   [ring.util.response]

   [stresty.operations.core :as ops]))

(defn form-decode [s]
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

(defmulti rest-op (fn [ztx op request] (:zen/name op)))

(defmethod rest-op :default
  [ztx op request]
  {:status 500
   :body {:message (str (:zen/name op) " is not implemented.")}})

(defmethod rest-op 'sty/index-op
  [ztx op request]
  {:status 200
   :body {:message "Welcome to stesty!"}})

(defmethod rest-op 'sty/rpc-op
  [ztx rest-op {{meth :method :as args} :body}]
  (println "Method " meth)
  (println "ARGS: " args)
  (if-let [rpc-op (and meth (let [meth (symbol meth)] (zen/get-symbol ztx meth)))]
    {:status 200
     :body (ops/call-op ztx rpc-op args)}
    {:status 404
     :body {:message (str "Operation " meth " is not registered")}}))

(defmethod rest-op 'sty/get-rpc-op
  [ztx rest-op {{meth :method :as params} :params}]
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
  (let [{routes :routes} (or (:routes @ztx) (zen/get-symbol ztx 'sty/api))]
    (when-let [{op-name :match params :params :as route} (match-route meth uri routes)]
      (if-let [op (zen/get-symbol ztx op-name)]
        {:op op :params params}
        {:error {:message (format "rest-op %s is not registered" op-name)}}))))

(defn dispatch [ztx request]
  (let [{op :op params :params error :error} (route ztx request)]
    (cond error {:status 500 :body error}
          op (rest-op ztx op (assoc request :route-params params))
          :else {:status 404 :body {:message (str "Route " (:request-method request) " " (:uri request) " is not found. Routes " (zen/get-symbol ztx 'sty/api))}})))

(defn start-server [ztx {port :port}]
  (let [port (or port 4174)
        handler (-> (mk-handler ztx dispatch)
                    (wrap-static))
        srv (server/run-server handler {:port port})]
    (fmt/emit ztx {:type 'http.server/start :port port})
    (swap! ztx assoc :server srv)))

(defn stop-server [ztx]
  (when-let [srv (get @ztx :server)]
    (fmt/emit ztx {:type 'http.server/stop})
    (srv)
    (swap! ztx dissoc :server)))

(defn restart-server [ztx opts]
  (stop-server ztx)
  (start-server ztx opts))
  

(comment
  (def ztx (zen/new-context {}))
  (zen/read-ns ztx 'sty)

  (zen/read-ns ztx 'aidbox.sci)

  (stop-server ztx)
  
  (:errors @ztx)

  (restart-server ztx {})

  (def cases (zen/get-symbol ztx 'aidbox.sci/sample))
  

  

  )
