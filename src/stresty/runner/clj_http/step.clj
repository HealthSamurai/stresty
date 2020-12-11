(ns stresty.runner.clj-http.step
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [matcho]
            [b64]
            [clj-http.client :as http]))

;; (remove-ns 'stresty.runner.clj-http.step)

(defn parse-json-or-leave-string [s]
  (try
    (json/parse-string s keyword)
    (catch com.fasterxml.jackson.core.JsonParseException e
      s)))

(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})

(defn default-req-params [ctx]
  (let [config (:config ctx)]
    {:url (:url config)
     :redirect-strategy :none
     :throw-exceptions false
     :headers (merge {"content-type" "application/json"} (:headers config))}))

#_(auth {:config {:agents {:default {:client-id "stresty"
                                   :client-secret "stresty"
                                   :type 'stresty/basic-auth}}}}
      {}
      :default)

(defmulti auth (fn [ctx _ agent-name]
                 (get-in ctx [:config :agents agent-name :type])))

(defmethod auth 'stresty/basic-auth [ctx req-opts agent-name]
  (let [agnt (get-in ctx [:config :agents agent-name])]
    {:req-opts (update req-opts :headers
                       assoc "authorization" (str "Basic " (b64/encode (str (:client-id agnt) ":" (:client-secret agnt)))))
     :ctx      ctx}))

(defn request [opts]
  (http/request opts))

(defn get-user-data [req-opts]
  (let [{body :body :as resp} (-> req-opts
                                  http/request)
        json-resp             (json/parse-string body keyword)]
    (:access_token json-resp)))

(defmethod auth 'stresty.aidbox/auth-token [ctx req-opts agent-name]
  (if-let [token (get-in ctx [:config :agents agent-name :token])]
    {:req-opts (update req-opts :headers assoc "authorization" (str "Bearer " token))}
    (let [agnt (get-in ctx [:config :agents agent-name])
          body (json/generate-string {:username      (:username agnt)
                                      :password      (:password agnt)
                                      :client_id     (:client-id agnt)
                                      :client_secret (:client-secret agnt)
                                      :grant_type    "password"})
          url (-> ctx
                  (get-in [:config :url])
                  (str "/auth/token"))
          resp (-> (default-req-params ctx)
                   (assoc :body body)
                   (assoc :request-method :post)
                   (assoc :url url)
                   request)
          json-resp (json/parse-string (resp :body) keyword)
          token (:access_token json-resp)]
      (if (= (:status resp) 200)
        {:req-opts (update req-opts :headers
                           assoc "authorization" (str "Bearer " token))
         :ctx (assoc-in ctx [:config :agents agent-name :token] token)}
        resp))))


;;   {:url              (str (:base-url ctx) "/auth/token")
;;    :method           :post
;;    :throw-exceptions false
;;    :headers          {"content-type" "application/json"}
;;    :body             (json/generate-string {:username      (:user-id ctx)
;;                                             :password      (:user-secret ctx)
;;                                             :client_id     (:auth-client-id ctx)
;;                                             :client_secret (:auth-client-secret ctx)
;;                                             :grant_type    "password"})}
;;   (str (:base-url ctx) "/auth/token")
;;   )


(defmulti run-step (fn [ctx step] (:type step)))

(defn request [opts]
  (try
    (http/request opts)
    (catch Exception e
      (Throwable->map e))))

(defmethod run-step 'stresty/http-step [{:keys [config] :as ctx} step]
  (let [method     (first (filter meths (keys step)))
        url        (str (get-in ctx [:config :url]) (get step method))
        body       (if (:body step) (if (string? (:body step)) (:body step) (json/generate-string (:body step))))
        req-opts   (cond-> (merge (default-req-params ctx) {:url url :request-method (keyword (str/lower-case (name method)))})
                     body
                     (assoc :body body))
        agent-name (get step :agent :default)
        {req-opts :req-opts
         new-ctx  :ctx}
        (if (get-in ctx [:config :agents agent-name]) (auth ctx req-opts agent-name) (throw (ex-info (str "No config for agent " (name agent-name)) {:agent-name agent-name})))
        ctx        (or new-ctx ctx)
        resp       (http/request req-opts)
        resp
        (cond-> {:status (:status resp)
                 :headers (reduce (fn [m [k v]] (assoc m (str/lower-case k) v)) {} (:headers resp))}
          (:body resp)
          (assoc :body (parse-json-or-leave-string (:body resp))))
        errs       (if-let [m (:match step)] (matcho/match nil resp m) [])]
    {:response resp
     :errors   errs}))

(defmethod run-step 'stresty.aidbox/desc-step [ctx step]
  {})

(defmethod run-step 'stresty.aidbox/sql-step [ctx step]
  (prn "SQL: " (str (:sql step)))
  (run-step ctx {:type  'stresty/http-step
                 :POST  "/$sql"
                 :body  (pr-str (:sql step))
                 :match {:status 200}}))

(defmethod run-step 'stresty.aidbox/truncate-step [ctx step]
  (let [sql (str "TRUNCATE "
                 (str/join ", "
                           (map (fn [x] (str (if (keyword? x) (name x) x)))
                                (:truncate step))))]
    (run-step ctx {:type 'stresty.aidbox/sql-step
                   :sql  sql})))


