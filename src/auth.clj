(ns auth
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [b64]))

(defn- get-admin-data [ctx]
  {:headers {"Authorization"
             (str "Basic "
                  (b64/encode
                    (str (:client-id ctx) ":" (:client-secret ctx))))}})

(defn- auth-user-req [ctx]
  {:url              (str (:base-url ctx) "/auth/token")
   :method           :post
   :throw-exceptions false
   :headers          {"content-type" "application/json"}
   :body             (json/generate-string {:username      (:user-id ctx)
                                            :password      (:user-secret ctx)
                                            :client_id     (:auth-client-id ctx)
                                            :client_secret (:auth-client-secret ctx)
                                            :grant_type    "password"})})

(defn get-user-data [ctx]
  (let [{body :body :as resp} (-> ctx
                                  auth-user-req
                                  http/request)
        json-resp             (json/parse-string body true)
        access-token          (:access_token json-resp)]
    (merge {:headers {"Authorization" (str "Bearer " access-token)}}
           (:userinfo json-resp))))

(defn- get-auth-headers [ctx]
  (let [headers {:admin (get-admin-data ctx)}]
    (cond-> headers
      (every? ctx [:auth-client-id :auth-client-secret :auth-user :auth-user-password])
      (assoc :user (get-user-data ctx)))))

(defn add-auth-data [ctx]
  (cond-> ctx
    (every? ctx [:auth-client-id :auth-client-secret :auth-user :auth-user-password])
    (assoc :user (get-user-data ctx))

    true
    (merge {:admin (get-admin-data ctx)})))

(comment

  (get-auth-headers {:base-url      "http://access-policy-box.aidbox.io"
                     :client-id     "postman"
                     :client-secret "postman"})

  (get-auth-headers {:base-url           "http://access-policy-box.aidbox.io"
                     :client-id          "postman"
                     :client-secret      "postman"
                     :auth-client-id     "myapp"
                     :auth-client-secret "verysecret"
                     :auth-user          "patient-user"
                     :auth-user-password "admin"})
  (clojure.pprint/pprint
    (add-auth-headers {:base-url           "http://access-policy-box.aidbox.io"
                       :client-id          "postman"
                       :client-secret      "postman"
                       :auth-client-id     "myapp"
                       :auth-client-secret "verysecret"
                       :auth-user          "patient-user"
                       :auth-user-password "admin"}))

  )


