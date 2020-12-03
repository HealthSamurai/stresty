(ns runner
  (:require [clj-yaml.core :as yaml]
            [clj-http.client :as http]
            [matcho]
            [pprint]
            [colors]
            [clojure.java.io :as io]
            [cheshire.core]
            [b64]
            [template]

            [clojure.string :as s]
            [clojure.string :as str]
            [auth]
            [stresty.runner.clj-http.step :as step]))

(defmacro v [ctx & cmds]
  (let [do-cmds (conj cmds 'do)]
    `(when (>= (:verbosity ~ctx) 1)
       ~do-cmds)))

(defmacro vv [ctx & cmds]
  (let [do-cmds (conj cmds 'do)]
    `(when (>= (:verbosity ~ctx) 2)
       ~do-cmds)))

(defmacro i [ctx & cmds]
  (let [do-cmds (conj cmds 'do)]
    `(when (:interactive ~ctx)
       ~do-cmds)))

(defmacro i-or-vv [ctx & cmds]
  (let [do-cmds (conj cmds 'do)]
    `(when (or (>= (:verbosity ~ctx) 2) (:interactive ~ctx))
       ~do-cmds)))

#_(defn parse-uri [conf step]
    (let [method (reduce (fn [_ method]
                           (when (get step method)
                             (reduced method))) nil meths)]
      (update step method #(template/render conf %))))

(defn run-test-case [ctx {:keys [steps :zen/name] :as test-case}]
  (println "run test case" name)
  (reduce
   (fn [ctx step]
     (let [errors (-> (get-in ctx [:results (keyword name)])
                      first
                      :errors)]
       (println "Errors: " errors)
       (if-not (empty? errors)
         ctx
         (step/run-step ctx step))))
   (assoc ctx :current-case (keyword name))
   steps))

(defn sum-for-test-case [{:keys [steps]}]
  {:passed-tests  (count (filter #(-> % :status (= "passed")) steps))
   :failed-tests  (count (filter #(-> % :status (= "failed")) steps))
   :skipped-tests (count (filter #(-> % :status (= "skipped")) steps))})

(defn sum-for-test-cases [test-cases]
(reduce (fn [a b] {:passed-tests  (+ (:passed-tests a) (:passed-tests b))
                   :failed-tests  (+ (:failed-tests a) (:failed-tests b))
                   :skipped-tests (+ (:skipped-tests a) (:skipped-tests b))})
          (map sum-for-test-case test-cases)))

(defn get-summary [{test-cases :test-cases}]
  (let [failed-tests (filter :failed? test-cases)
        passed-tests (filter #(-> % :failed? not) test-cases)]
    {:failed-tests       failed-tests
     :passed-tests       passed-tests
     :count-failed-tests (count failed-tests)
     :count-all-tests    (count test-cases)
     :count-passed-tests (count passed-tests)}))

(defn run [{:keys [test-cases config] :as ctx}]
  (reduce run-test-case ctx test-cases)
  #_(let [result  (reduce run-test-case ctx test-cases)
          sum     (sum-for-test-cases (:test-cases result))
          summary (get-summary result)
          passed? (zero? (:count-failed-tests summary))]

      (println)
      (println "Test results:" (:passed-tests sum) "passed,")
      (println "             " (:failed-tests sum) "failed,")
      (println "             " (:skipped-tests sum) "skipped.")

      (when (not passed?)
        (println "Failed tests:")
        (doseq [test-case (:test-cases result)]
          (println (:id test-case))))

      (assoc result :passed? (zero? (:failed-tests sum)))))

(comment

  (run-file {:base-url "http://boxik.aidbox.app"} "stresty.tests.core")

  (def ctx {:interactive        false
            :verbosity          0
            :base-url           "http://access-policy-box.aidbox.io"
            :client-id          "stresty"
            :client-secret      "stresty"
            :auth-client-id     "myapp"
            :auth-client-secret "verysecret"
            :user-id            "patient-user"
            :user-secret        "admin"})

  (def ctx* (merge ctx (auth/add-auth-data ctx)))

  (prn ctx*)

  (clojure.pprint/pprint (run ctx* ["test/sample-1.edn"]))

  (def r (run ctx*
           ["resources/stresty/tests/core.edn"]))

  (def tc (-> r
              :test-cases
              first))

  (:filename tc)

  (sum-for-test-case steps)

  (clojure.pprint/pprint r)

  (-> (run {:interactive false :verbosity 2 :base-url "http://localhost:8888" :basic-auth "cm9vdDpzZWNyZXQ="} [#_"test/w.yaml" "test/w.yaml"])
      :failed))
