(ns http
  (:require [clj-http.lite.client]))


(defn request [ctx req]
  (clj-http.lite.client/request
   (merge 
    {:throw-exceptions false}
    req)))

