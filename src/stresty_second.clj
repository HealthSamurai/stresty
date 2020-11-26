(ns strest-second
  (:require [zen.core :as zen]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [b64]
            [org.httpkit.client :as http]))

(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})

(defmulti auth-headers (fn [ctx user] (:type agent)))

(defmethod auth-headers 'stresty/basic-auth [ctx user]
  (str "Basic " (b64/encode (str (:client-id user) ":" (:client-secret user)))))

(defmethod auth-headers 'stresty/resource-owner-auth [ctx user]
  {:url              (str (:base-url ctx) "/auth/token")
   :method           :post
   :throw-exceptions false
   :headers          {"content-type" "application/json"}
   :body             (json/generate-string {:username      (:user-id user)
                                            :password      (:user-secret user)
                                            :client_id     (:auth-client-id user)
                                            :client_secret (:auth-client-secret user)
                                            :grant_type    "password"})})

(defn mk-req [ctx step]
  (let [method (first (filter meths (keys step)))
        url    (get step method)
        agent (get step :agent "default")
        auth-token (auth-headers ctx agent)
        ctx (update-in ctx [:agents agent] auth-token)]
    (merge (cond-> {:url (str (get-in ctx [:config :url]) url)
                    :throw-exceptions false
                    :headers (merge {"content-type" "application/json"} {"authorization" auth-token})
                    :path url
                    :request-method (keyword (str/lower-case (name method)))}
             (:body step)
             (assoc :body (json/generate-string (:body step)))) opts)
    )

  (if-let [method (first (filter meths (keys step)))]
    (let [
          opts  (select-keys step [:headers :auth])
          agent (keyword (:agent step))]
      )))

(defn run-step [ctx step]
  (mk-req step)

  )

(comment

  (zen/load-ns! )

  (def ctx (zen/new-context))

  (zen/read-ns ctx 'stresty)

  ctx


  (def test
    {:steps
     [{:GET "/User/user-1"
       :match {:status 200}
       :ctx {:agents [:id]}}
      {:GET "/Patient/pt-1"
       :match {:status 200}}]})


  (reduce (fn [ctx step]
            (run-step ctx step))
          {:url "https://little.aidbox.app"
           :agent {"default" {:type 'stresty/basic-auth
                              :username "basic"
                              :password "secret"}
                   "user" {:type 'stresty/user-login
                           :client-id "..."
                           :client-secret "..."
                           :user "user-1"
                           :password "password"}}}
          (:steps test))


  )
