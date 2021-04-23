(ns stresty.actions.core
  (:require [clj-http.lite.client :as http]
            [cheshire.core]))

(defmulti run-action (fn [ztx ctx args] (or (:type args) 'sty/http)))

(defmethod run-action :default
  [ztx ctx args]
  {:error {:message (str "Action " (:type args) " is not implemented!")}})

(defmethod run-action 'sty/http
  [ztx {env :env case :case state :state} args]
  (let [url  (str (:base-url env) (:url args))
        resp (-> (http/request {:method (:method args)
                                :url url
                                :headers {"content-type" "application/json"}
                                :body (when-let [b (:body args)]
                                        (cheshire.core/generate-string b))})
                 (update :body (fn [x] (when x (cheshire.core/parse-string x keyword)))))]
    (println "\n" ::http (:method args) url)
    resp))
