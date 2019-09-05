(ns core
  (:require [clj-http.lite.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn do-get [ctx path & [params]]
  (let [res (http/request
             {:url (str (:base-url ctx) path)
              :method :get
              :throw-exceptions false
              :basic-auth [(:client-id ctx) (:client-secret ctx)]
              :query-params (merge {:_format "json"} (or params {}))})]
    {:status (:status res)
     :body (json/parse-string (:body res) keyword)}))

(defn do-patch [ctx path body]
  (let [res (http/request
             {:url (str (:base-url ctx) path)
              :method :post
              :throw-exceptions false
              :headers {"x-http-method-override" "patch"
                        "content-type" "application/json"}
              :basic-auth [(:client-id ctx) (:client-secret ctx)]
              ;; :query-params {:_method "merge-patch"}
              :body (json/generate-string body)})]
    {:status (:status res)
     :body (json/parse-string (:body res) keyword)}))

(defn do-post [ctx path body]
  (let [res (http/request
             {:url (str (:base-url ctx) path)
              :method :post
              :throw-exceptions false
              :headers {"content-type" "application/json"}
              :basic-auth [(:client-id ctx) (:client-secret ctx)]
              ;; :query-params {:_method "merge-patch"}
              :body (json/generate-string body)})]
    {:status (:status res)
     :body (json/parse-string (:body res) keyword)}))

(defn do-put [ctx path body]
  (let [res (http/request
             {:url (str (:base-url ctx) path)
              :method :put
              :throw-exceptions false
              :headers {"content-type" "application/json"}
              :basic-auth [(:client-id ctx) (:client-secret ctx)]
              :body (json/generate-string body)})]
    {:status (:status res)
     :body (json/parse-string (:body res) keyword)}))


(defn -main [& [args]]
  (let [ctx {:base-url      (System/getenv "AIDBOX_URL")}]
    (println "HEllo" ctx)))

(comment


  (-main )

  )

