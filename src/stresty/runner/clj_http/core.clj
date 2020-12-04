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
  (do
    (def ztx (zen/new-context))
    (clojure.pprint/pprint (zen/read-ns ztx 'user))
    (zen/get-symbol ztx 'config))

  (def result (run-case ztx {:url    "http://access-policy-box.aidbox.io/"
                             :agents {:default {:type          'stresty/basic-auth
                                                :client-id     "stresty"
                                                :client-secret "stresty"}
                                      :user    {:type          'stresty.aidbox/auth-token
                                                :username      "patient-user"
                                                           :password      "admin"
                                                :client-id     "myapp"
                                                :client-secret "verysecret"
                                                }}}
                        'user/create-patient))

  (:results)
  
  ;; => {:stresty.runner.clj-http.core/case
;;     {:desc "Create Patient",
;;      :zen/tags #{stresty/case},
;;      :steps
;;      [{:truncate [:Patient], :type stresty.aidbox/truncate-step}
;;       {:type stresty/http-step,
;;        :POST "/Patient",
;;        :body {:name [{:given ["John"], :family "Smith"}], :id "new-patient"},
;;        :match {:status 201}}
;;       {:type stresty/http-step,
;;        :agent :user,
;;        :GET "/Patient/new-patient",
;;        :match {:status 200, :body {:id "new-patient"}}}],
;;      :zen/file "/home/u473t8/Projects/health-samurai/stresty/resources/user.edn",
;;      :zen/name user/create-patient},
;;     :stresty.runner.clj-http.core/steps nil,
;;     :config
;;     {:url "http://access-policy-box.aidbox.io/",
;;      :agents
;;      {:default
;;       {:type stresty/basic-auth, :client-id "stresty", :client-secret "stresty"},
;;       :user
;;       {:type stresty.aidbox/auth-token,
;;        :username "patient-user",
;;        :password "admin",
;;        :client-id "myapp",
;;        :client-secret "verysecret",
;;        :token "OWI1MzlkOGItZDMxYS00YWFkLTlhNTQtM2NlZTMxMWUyYTFj"}}},
;;     :stresty.runner.clj-http.core/last-result
;;     {:resp
;;      {:status 200,
;;       :headers
;;       {"x-duration" "5",
;;        "server" "http-kit",
;;        "via" "1.1 google",
;;        "content-type" "application/json",
;;        "content-length" "203",
;;        "connection" "close",
;;        "etag" "660",
;;        "date" "Wed, 02 Dec 2020 15:47:48 GMT",
;;        "last-modified" "Wed, 02 Dec 2020 15:47:47 GMT",
;;        "x-request-id" "fa30f82b-041d-4e20-9197-1b58cccaca27",
;;        "cache-control" "no-cache"},
;;       :body
;;       {:name [{:given ["John"], :family "Smith"}],
;;        :id "new-patient",
;;        :resourceType "Patient",
;;        :meta
;;        {:lastUpdated "2020-12-02T15:47:47.920183Z",
;;         :createdAt "2020-12-02T15:47:47.920183Z",
;;         :versionId "660"}}},
  ;;      :errors []}})

  )
