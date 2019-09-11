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

(comment

  (defn wow [{:keys [h]}] h)

(run-step {} {:h "wow"})

  )

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


(defn run-step [ctx step]
  (println "run-step:" (:id step))
  (cond
    (:failed ctx)
    ctx

    :else
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
        (when (verbose-enough? ctx 2)
          (when (:desc step)
            (println (str (or (:desc step) "") "         \n")))
          (println (colors/yellow (colors/bold (name (:method req)))) (:path req))
          (println "Response")
          (println (yaml/generate-string resp)))

        (if (empty? errs)
          ctx
          (do
            (when (verbose-enough? ctx 2)
              (pprint/pretty {:ident 0 :path [] :errors errs} resp))
            (assoc ctx :failed true :errors errs))))
      (assoc ctx :failed true :message "Cannot create requrest") 
      )))

(comment

  (run-step {:verbosity 2 :base-url "http://localhost:8888" :basic-auth "cm9vdDpzZWNyZXQ="}
            {:id "wow" :GET "/Patient"})

  )

(defn run-test-case [ctx test-case]
  (println "run test case")
  (reduce #(run-step %1 %2) ctx (:steps test-case))
  )



(defn run-script [ctx script]
  (println (or (:id script) (:file script)))
  (doseq [step (:steps script)]
    (when-let [req (mk-req ctx step)]
      (when (:desc step)
        (println (str (or (:desc step) "") "         \n")))
      (println (colors/yellow (colors/bold (name (:method req)))) (:path req))
      (when (:body step)
        (pprint/pretty {:ident 0} {:body (:body step)})
        (println))
      (println)
      (let [{s :status :as resp} (http/request req)
            b (when-let [b (:body resp)]
                (cheshire.core/parse-string b keyword))
            resp (cond-> {:status s} b (assoc :body b))
            errs (when-let [m (:match step)]
                   (->> (matcho/match resp m)
                        (reduce (fn [acc {pth :path exp :expected}]
                                  (assoc-in acc pth {:expected exp})
                                  ) {})))]

        (pprint/pretty {:ident 0 :path [] :errors errs} resp)
        (if (empty? errs)
          (println "\n" (colors/green "OK!"))
          #_(println "\n" (colors/red (pr-str errs)))))
      (println (colors/dark (colors/white "\n---------------------------------\n")))
      )))

(comment


  )

(defn run-file [ctx f]
  (let [ctx (assoc ctx :files [])]
    (let [script (assoc (yaml/parse-string (slurp f)) :files f)]
      (when (valid? ctx script)
        (let [result (run-test-case ctx script)]
          (update-in ctx [:files] #(conj % result)))))))

(defn get-summary [{tests :files}]
  {:failed-tests (filter :failed tests)
         :passed-tests (filter #(-> % :failed not) tests)
         :count-failed-tests (count failed-tests)
         :count-all-tests (count tests)
         :count-passed-tests (count passed-tests)}
  )

(defn run [ctx files]

  (let [result (reduce run-file ctx files)
        summary (get-summary result)]

    (if (zero? (:count-failed-tests summary))
      (println "All" count-all-tests "passed.")
      (println "Result:" count-passed-tests "passed," count-failed-tests "failed."))

    (clojure.pprint/pprint result)

    result)
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
