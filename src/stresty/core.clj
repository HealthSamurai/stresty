(ns stresty.core
  (:require [zen.core :as zen]
            [clojure.string :as str]
            [runner :refer :all]))


(def ctx (zen/new-context))

(defn no-errors! [ctx]
  (when-not (empty? (:errors @ctx))
    (println (str/join "\n" (mapv pr-str (:errors @ctx))))
    (assert (empty? (:errors @ctx)) "See STDOUT for errors")))

(zen/read-ns ctx 'stresty)

(no-errors! ctx)

(zen/read-ns ctx 'stresty.tests.core)

(no-errors! ctx)

(doseq [case-id (zen/get-tag ctx 'stresty/case)]
  (let [case (zen/get-symbol ctx case-id)]
    (println "Run case\n" (:desc case))
    (doseq [stp (:steps case)]
      (prn (exec-step {:conf {:base-url "http://access-policy-box.aidbox.io"
                              :client-id "stresty"
                              :client-secret "stresty"
                              :authorization-type "Basic"
                              :interactive false
                              :verbosity 1}}
                      stp))
       )))

(->
 (zen/validate
  ctx #{'stresty/case}
  {:desc "Wrong name"
   :steps
   [{:GET "/Patient"
     :headers "string"
     :match {:status "200"
             :body {:id 'stresty/unknown?}}}]})
 (:errors)
 (->> (str/join "\n"))
 (println))

