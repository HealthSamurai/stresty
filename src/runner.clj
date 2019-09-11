(ns runner
  (:require [clj-yaml.core :as yaml]
            [clj-http.lite.client :as http]
            [matcho]
            [pprint]
            [colors]
            [clojure.java.io :as io]
            [cheshire.core]))

(defn valid? [ctx script]
  true)

(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})

(defn get-auth-headers [ctx]
  (if-let [basic-auth (:basic-auth ctx)]
    (do 
        {"Authorization" (str "Basic " basic-auth)})))

(defn mk-req [ctx step]
  (if-let [method (first (filter meths (keys step)))]
    (let [url (get step method)
          opts (select-keys step [:headers :auth])]
      (merge (cond->
                 {:url (str (:base-url ctx) url)
                  :throw-exceptions false
                  :headers (merge {"content-type" "application/json"} (get-auth-headers ctx))
                  :path url
                  :method method}
               (:body step) (assoc :body (cheshire.core/generate-string (:body step)))
               (and (:client-id ctx) (:client-secret ctx))
               (assoc :basic-auth [(:client-id ctx) (:client-secret ctx)])
               ) opts))

    (println "Warn: step should contain one of methods" meths ", but" (str "\n" (yaml/generate-string step)))))


(defn run-test-scenario [ctx script]
  #_(println (or (:id script) (:file script)))
  (doseq [step (:steps script)]
    (when-let [req (mk-req ctx step)]
      (when (:desc step)
        (println (str (or (:desc step) "") "         \n")))
      (println (colors/yellow (colors/bold (name (:method req)))) (:path req))
      #_(when (:body step)
        (pprint/pretty {:ident 0} {:body (:body step)})
        (println))
      (let [{s :status :as resp} (http/request req)
            b (when-let [b (:body resp)]
                (cheshire.core/parse-string b keyword))
            resp (cond-> {:status s} b (assoc :body b))
            errs (when-let [m (:match step)]
                   (->> (matcho/match resp m)
                        (reduce (fn [acc {pth :path exp :expected}]
                                  (assoc-in acc pth {:expected exp})
                                  ) {})))]

        #_(pprint/pretty {:ident 0 :path [] :errors errs} resp)
        (println errs)
        errs
        #_(if (empty? errs)
          (println "\n" (colors/green "OK!"))
          #_(println "\n" (colors/red (pr-str errs)))))
      #_(println (colors/dark (colors/white "\n---------------------------------\n")))
      )))

(defn verbose-enough? [ctx expected-lvl]
  (>= (or (:verbosity ctx) 0) expected-lvl))

(defn v? [ctx] (verbose-enough? ctx 1))

(defn vv? [ctx] (verbose-enough? ctx 2))

(defn run-step [ctx step]
  (cond
    ;; skip next steps if some previous one in the test-case was failed
    (:failed ctx)
    (do
      (when (vv? ctx)
        (println "skip step " (:id step)))
      ctx)

    :else
    (do
      (when (vv? ctx)
        (println "run step:" (:id step)))
      (if-let [req (mk-req ctx step)]
        (let [{s :status :as resp} (http/request req)
              b (when-let [b (:body resp)]
                  (cheshire.core/parse-string b keyword))
              resp (cond-> {:status s} b (assoc :body b))
              errs (when-let [m (:match step)]
                     (->> (matcho/match resp m)
                          (reduce (fn [acc {pth :path exp :expected}]
                                    (assoc-in acc pth {:expected exp})
                                    ) {})))]
          (when (vv? ctx)
            (when (:desc step)
              (println (str (or (:desc step) "") "         \n")))
            (println "Request")
            (println (colors/yellow (colors/bold (name (:method req)))) (:path req))
            (when (:body req)
              (println (yaml/generate-string (:body req))))
            (println "Response")
            (println (yaml/generate-string resp)))

          (if (empty? errs)
            ctx
            (assoc ctx
                   :failed true
                   :errors errs
                   :resp resp)))
        (assoc ctx :failed true :message "Cannot create requrest") 
        ))))

(comment

  (run-step {:verbosity 2 :base-url "http://localhost:8888" :basic-auth "cm9vdDpzZWNyZXQ="}
            {:id "wow" :GET "/Patient"})

  )

(defn run-test-case [ctx test-case]
  (when (v? ctx)
    (println "run test case" (:id test-case)))
  (let [result (reduce #(run-step %1 %2) ctx (:steps test-case))]
    (when (:errors result)
      (pprint/pretty {:ident 0 :path [] :errors (:errors result)} (:resp result)))
    result))


(defn run-file [ctx f]
  (let [test-case (assoc (yaml/parse-string (slurp f)) :files f)
        ctx (assoc ctx :files [])]
    (when (valid? ctx test-case)
      (let [result (run-test-case ctx test-case)]
        (update-in ctx [:files] #(conj % result))))))

(defn get-summary [{tests :files}]
  (let [failed-tests (filter :failed tests)
        passed-tests (filter #(-> % :failed not) tests)]
    {:failed-tests failed-tests
     :passed-tests passed-tests
     :count-failed-tests (count failed-tests)
     :count-all-tests (count tests)
     :count-passed-tests (count passed-tests)}))

(defn run [ctx files]

  (let [result (reduce run-file ctx files)
        summary (get-summary result)
        failed? (not (zero? (:count-failed-tests summary)))]

    (println)
    (if failed?
      (println (:count-passed-tests summary) "passed,"
               (:count-failed-tests summary) "failed.")
      (println "All" (:count-all-tests summary) "passed."))

    #_(clojure.pprint/pprint result)

    (assoc result :failed failed?))
  #_(doseq [f files]
    (println "Configuration:")
    (clojure.pprint/pprint ctx)
    (println)
    (println "Read " f)
    (run-file ctx f)))

(comment

  (run-file {:base-url "http://ya.ru"} "test/ya.yaml")

  (run-file {:base-url "http://main.aidbox.app"} "test/sample.yaml")

  (-> (run {:verbosity 2 :base-url "http://localhost:8888" :basic-auth "cm9vdDpzZWNyZXQ="} [#_"test/w.yaml" "test/w.yaml"])
      :failed)

  )
