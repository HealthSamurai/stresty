(ns http
  (:require [clj-http.lite.client]))


(defn request [ctx req]
  (clj-http.lite.client/request
   (merge 
    {:throw-exceptions false
     :basic-auth [(:client-id ctx) (:client-secret ctx)]}
    req)))

