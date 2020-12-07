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
  (println "Run test case" name)
  (map (fn [step]
         (-> (step/run-step ctx step)
             (assoc :case-name name))) steps))

#_(defn sum-for-test-case [{:keys [test-cases results] :as ctx}]
  {:passed-tests  (count (filter #(-> % :status (= "passed")) steps))
   :failed-tests  (count (filter #(-> % :status (= "failed")) steps))
   :skipped-tests (count (filter #(-> % :status (= "skipped")) steps))})

(def results {:results #:user{:create-patient
                              [{:response 1
                                :errors [{:expected 200}]}
                               {:response 2
                                :errors []}]

                              :put-patient
                              [{:response 2}
                               {:response 3}
                               {:response 4}]}})


(defn sum-for-test-cases [{:keys [test-cases results] :as ctx}]
  (reduce (fn [acc r]
            (conj acc  r))
          [] results))

(sum-for-test-cases results) 
;; => [[:user/create-patient
;;      [{:response 1, :errors [{:expected 200}]} {:response 2, :errors []}]]
;;     [:user/put-patient [{:response 2} {:response 3} {:response 4}]]]
(defn get-summary [{test-cases :test-cases}]
  (let [failed-tests (filter :failed? test-cases)
        passed-tests (filter #(-> % :failed? not) test-cases)]
    {:failed-tests       failed-tests
     :passed-tests       passed-tests
     :count-failed-tests (count failed-tests)
     :count-all-tests    (count test-cases)
     :count-passed-tests (count passed-tests)}))

(defn run [{:keys [test-cases config] :as ctx}]
  (map (partial run-test-case ctx) test-cases)
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


