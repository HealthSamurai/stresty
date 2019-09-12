(ns runner
  (:require [clj-yaml.core :as yaml]
            [clj-http.lite.client :as http]
            [matcho]
            [pprint]
            [colors]
            [clojure.java.io :as io]
            [cheshire.core]
            [b64]
            ))

(defn valid? [ctx script]
  true)

(defmacro v [ctx & cmds]
  (let [do-cmds (conj cmds 'do)]
    `(when (>= (:verbosity ~ctx) 1)
        ~do-cmds)))

(defmacro vv [ctx & cmds]
  (let [do-cmds (conj cmds 'do)]
    `(when (>= (:verbosity ~ctx) 2)
        ~do-cmds)))

(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})

(defn get-auth-headers [ctx]
  (let [auth-type (:authorization-type ctx)]
    (cond
      (= auth-type "Basic")
      {"Authorization" (str "Basic "
                            (b64/encode
                             (str (:client-id ctx) ":" (:client-secret ctx))))}

      (nil? auth-type)
      {}

      :else
      (throw (ex-info (str "Unknown authorization type " auth-type) {})))))

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


(defn verbose-enough? [ctx expected-lvl]
  (>= (or (:verbosity ctx) 0) expected-lvl))

(defn run-step [ctx step]
  (cond
    ;; skip next steps if some previous one in the test-case was failed
    (:failed ctx)
    (do
      (vv ctx
          (println "skip step " (:id step)))
      ctx)

    :else
    (do
      (vv ctx
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
          (vv ctx
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


(defn run-test-case [ctx test-case]
  (v ctx
     (println "run test case: " (:id test-case)))
  (let [result (reduce #(run-step %1 %2) ctx (:steps test-case))]

    (when (:errors result)
      (pprint/pretty {:ident 0 :path [] :errors (:errors result)} (:resp result)))
    (assoc result :id (:id test-case))))


(defn run-file [ctx f]
  (let [test-case (assoc (yaml/parse-string (slurp f))
                         :filename f)
        ctx (assoc ctx :files [] :filename f)]
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
    (println)
    (if failed?
      (do
        (println (:count-passed-tests summary) "passed,"
                 (:count-failed-tests summary) "failed.")
        (println "Failed tests:")
        (doseq [test-case (:files result)]
          (println (:id test-case) (str "(" (:filename result) ")"))))
      (println "All" (:count-all-tests summary) "passed."))

    (assoc result :failed failed?))
  )

(comment

  (run-file {:base-url "http://ya.ru"} "test/ya.yaml")

  (run-file {:base-url "http://main.aidbox.app"} "test/sample.yaml")

  (-> (run {:verbosity 2 :base-url "http://localhost:8888" :basic-auth "cm9vdDpzZWNyZXQ="} [#_"test/w.yaml" "test/w.yaml"])
      :failed)

  )
