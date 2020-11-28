(ns stresty.runner.clj-http.core
  (:require [zen.core :as zen]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [b64]
            [stresty.runner.clj-http.step]
            [org.httpkit.client :as http]))

;; (remove-ns 'stresty.runner.clj-http.core)

(defn find-case [ztx case-name]
  (let [case (zen/get-symbol ztx case-name)]
    (assert (contains? (:zen/tags case) 'stresty/case) "Case must be tagged with stresty/case")
    (println "found case " case-name)
    case))


(defn run-case [ztx config case]
  (let [case (if (symbol? case) (find-case ztx case) case)]
    (reduce
     (fn [ctx step]
       (println step)
       (if (::stop ctx)
         ctx
         (let [result (stresty.runner.clj-http.step/run-step ctx step)
               ctx (if (:ctx result) (:ctx result) ctx)
               result (dissoc result :ctx)
               ctx (-> ctx
                       (assoc ::last-result result)
                       (update ::steps (conj result)))]
           (if-not (empty? (:errors result))
             (assoc ctx ::stop true)
             ctx))))
     {::case case
      ::steps []
      :config config}
     (:steps case))))


(comment


  (+ 40 2)


  (do
    (def ztx (zen/new-context))
    (zen/read-ns ztx 'user))



  (run-case ztx {:url "https://little.aidbox.app"
                 :agents {:default {:type 'stresty/basic-auth
                                    :client-id "basic"
                                    :client-secret "secret"}}}
            'user/create-patient)



  (stresty.runner.clj-http.step/run-step
   nil
   (get-in
    (zen/get-symbol ztx 'user/create-patient)
    [:steps 0]))


  (get-in
   (zen/get-symbol ztx 'user/create-patient)
   [:steps 0 :type])
  ;; =>

  (zen/validate ztx ['stresty/step] {})

  (run-case ztx {:url "https://little.aidbox.app"
                 :agents {:default {:type 'stresty/basic-auth
                                    :client-id "basic"
                                    :client-secret "secret"}}}
            'user/my-scenario)



  (zen/load-ns! )


  (clojure.pprint/pprint)
  (zen/read-ns ztx 'user)

  ctx


  (zen/get-tag ctx 'stresty/case)


  (zen/get-symbol ctx 'user/my-scenario)




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
