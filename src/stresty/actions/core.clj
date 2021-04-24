(ns stresty.actions.core
  (:require [clj-http.lite.client :as http]
            [stresty.format.core :as fmt]
            [zen.core :as zen]
            [cheshire.core])
  (:import java.net.ConnectException))

(defmulti run-action (fn [ztx ctx args] (or (:type args) 'sty/http)))

(defmethod run-action :default
  [ztx ctx args]
  {:error {:message (format "Action '%s is not implemented!" (:type args))}})

(defn action [ztx ctx args]
  (let [tp (or (:type args) 'sty/http)]
    (if-let [schema (zen/get-symbol ztx tp)]
      (let [{errors :errors :as res} (zen/validate ztx #{'sty/action tp} args)]
        (if (empty? errors)
          (run-action ztx (assoc ctx :schema schema) args)
          {:error {:message "Wrong action parameters" :errors errors}}))
      {:error {:message (format "Action '%s is not defined " tp)}})))


(defmethod run-action 'sty/http
  [ztx {env :env case :case state :state} args]
  (let [url (str (:base-url env) (:url args))]
    (try
      (fmt/emit ztx {:type 'sty.http/request :method (:method args) :url url})
      (let [resp (-> (http/request {:method (:method args)
                                    :url url
                                    :throw-exceptions false
                                    :headers (merge
                                              (:headers args)
                                              {"content-type" "application/json"} )
                                    :body (when-let [b (:body args)]
                                            (cheshire.core/generate-string b))})
                     (update :body (fn [x] (when x (cheshire.core/parse-string x keyword)))))]
        
        {:result resp})
      (catch java.net.ConnectException _
        {:error {:message (format "Connection to %s is refused" url)}}))))
