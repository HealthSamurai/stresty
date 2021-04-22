(ns stresty.server
  (:require
   [zen.core :as zen]
   [org.httpkit.server :as server]
   [clojure.string :as str]
   [stresty.operations :as ops]))

(defn handler [ztx req]
  {:body "Hello"
   :status 200})

(defmulti rest-operation (fn [ztx op request] (:zen/name op)))

(defmethod rest-operation :default
  [ztx op request]
  {:status 500
   :body {:message (str (:zen/name op) " is not implemented")}})

(defmethod rest-operation 'sty/rest-op
  [ztx op request]
  {:status 200
   :body {}})

(defmethod rest-operation 'sty/rpc-op
  [ztx rest-op {{meth :method :as args} :body}]
  (if-let [rpc-op (and meth (let [meth (symbol meth)] (zen/get-symbol ztx meth)))]
    {:status 200
     :body (ops/call-op ztx rpc-op args)}
    {:status 404
     :body {:message (str "Operation " meth " is not registered")}}))

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
  "path [:get \"/your/path\"] or just \"/your/path\""
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
  (if-let [{op :op params :params} (route ztx request)]
    (do (println ::op op)
        (rest-operation ztx op (assoc request :route-params params)))
    {:status 404
     :body {:message (str "Route " (:request-method request) " " (:uri request) " is not found")}}))

(defn start-server [ztx opts]
  (let [port (or (:port opts) 4174)
        srv (server/run-server (fn [req] (handler ztx req)) {:port port})]
    (println ::server-started port)
    (swap! ztx :server srv)))

(defn stop-server [ztx]
  (when-let [srv (get @ztx :server)]
    (srv)
    (swap! ztx dissoc :server)))

(comment
  (def ztx (zen/new-context))

  (start-server ztx {})
  (stop-server ztx)

  )
