(ns runner
  (:require [clj-yaml.core :as yaml]
            [clj-http.client :as http]
            [matcho]
            [pprint]
            [colors]
            [clojure.java.io :as io]
            [cheshire.core]
            [b64]

            [clojure.string :as s]
            [clojure.string :as str]

            [zen.core :as zen]))

(def zen-ctx (zen/new-context))
(zen/read-ns zen-ctx 'stresty)


(defn zen-valid? [ctx]
  (when-not (empty? (:errors ctx))
    (println (str/join "\n" (mapv pr-str (:errors ctx))))
    (assert (empty? (:errors ctx)) "See STDOUT for errors"))
  (empty? (:errors ctx))
  )


(zen/read-ns zen-ctx 'stresty)

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


(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})

(defn get-auth-headers [ctx agent]
  (if (= agent nil)
    (get ctx :admin)
    (if-let [auth-header (get-in ctx [agent :headers])]
      auth-header
      (throw (ex-info (str "Not enough info to authorize " (name agent)
                           "\nExport AIDBOX_USER_CLIENT_ID, AIDBOX_USER_CLIENT_SECRET, "
                           "AIDBOX_USER, AIDBOX_USER_PASSWORD to use user agent") {}))
      )))

(defn mk-req [ctx step]
  (if-let [method (first (filter meths (keys step)))]
    (let [url   (get step method)
          opts  (select-keys step [:headers :auth])
          agent (keyword (:agent step))]
      (merge (cond-> {:url (str (:base-url ctx) url)
                      :throw-exceptions false
                      :headers (merge {"content-type" "application/json"} (get-auth-headers ctx agent))
                      :path url
                      :request-method (keyword (str/lower-case (name method)))}
               (:body step)
               (assoc :body (cheshire.core/generate-string (:body step)))) opts))
    (println "Warn: step should contain one of methods" meths ", but" (str "\n" (yaml/generate-string step)))))

(defn verbose-enough? [ctx expected-lvl]
  (>= (or (:verbosity ctx) 0) expected-lvl))

(defn exec-step [{:keys [conf steps] :as ctx} step]

  (cond
    ;; skip next steps if some previous one in the test-case was failed
    (or (:failed? ctx) (:skip step) (and (:only ctx) (not= (:only ctx) (:id step))))
    (do
      (println (colors/yellow "skip step") (:id step))
      (assoc step :status "skipped" :skipped? true))

    :else
    (do
      (i-or-vv conf
               (print (colors/yellow "run step") (:id step))
               (flush)
               (if (:interactive conf) (read-line) (println)))
      (if-let [req (mk-req conf step)]
        (let [{s :status :as resp} (http/request req)
              b                    (when-let [b (:body resp)]
                                     (cheshire.core/parse-string b keyword))
              resp                 (cond-> {:status s} b (assoc :body b))
              errs                 (when-let [m (:match step)]
                                     (->> (matcho/match resp m)
                                          (reduce (fn [acc {pth :path exp :expected}]
                                                    (assoc-in acc pth {:expected exp})) {})))]
          (vv conf
              (when (:desc step)
                (println (str (or (:desc step) ""))))
              (println "Request")
              (println (colors/bold (name (:method req))) (:path req))
              (when (:body req)
                (println (yaml/generate-string (:body req))))
              (println "Response")
              (println (yaml/generate-string resp)))

          (if (empty? errs)
            (do
              (i conf (println (colors/green "passed")))
              (assoc step :status "passed"))
            (do
              (println (colors/red "failed step") (:id step))
              (pprint/pretty {:ident 0 :path [] :errors errs} resp)
              (assoc step
                     :failed? true
                     :status "failed"
                     :errors errs
                     :resp resp
                     :req req))))
        (assoc step :failed? true :message "Cannot create requrest")))))

(defn get-id [test-case]
  (or (:id test-case) (:filename test-case)))

(defn parse-templated-string [conf template]
  (str/replace
    template
    #"\{([a-zA-Z0-9-_\.]+)\}"
    #(let [template-segments (-> % second (str/split #"\."))]
       (->> template-segments
            (mapv keyword)
            (get-in conf)
            str))))

(defn parse-uri [conf step]
  (let [method     (first (filter meths (keys step)))
        uri        (get step method)
        parsed-uri (parse-templated-string uri)]
    (assoc step method parsed-uri)))

(defn run-step [{:keys [conf steps] :as ctx} step]
  (let [step   (parse-uri conf step)
        result (exec-step ctx step)]
    (-> ctx
        (update
          :steps #(conj % result))
        (merge (if (:failed? result) {:failed?     true
                                      :failed-step (:id result)
                                      :resp        (:resp result)
                                      :errors      (:errors result)}
                   nil)))))

(defn find-only-step [ctx s]
(if (:only s)
  (:id s)
  ctx))

(defn- run-steps
  [conf test-case]
  (let [steps     (:steps test-case)
        only-step (reduce find-only-step nil steps)]
    (reduce run-step {:conf conf :steps [] :only only-step} steps)))

(defn run-test-case [conf test-case]
  (clojure.pprint/pprint test-case)
  (println "run test case" (:id test-case))

  (let [result (run-steps conf test-case)]
    (if (:failed? result)
      (println (colors/red "failed") (str (get-id test-case) "." (:failed-step result)))
      (println (colors/green "passed") (:id test-case)))

    #_(when (:errors result)
        (pprint/pretty {:ident 0 :path [] :errors (:errors result)} (:resp result)))
    (assoc result :id (:id test-case))))

(defn- file-extension [s]
(->> s
     (re-find #"\.([a-zA-Z0-9]+)$")
     last))

(defmulti load-test-case (fn [filename]
                           (let [extension (file-extension filename)]
                             (cond
                               (#{"yaml" "yml"} extension) :yaml
                               (= "edn" extension)         :zen))))

(defmethod load-test-case
  :yaml
  [filename]
  (-> filename
      slurp
      yaml/parse-string
      (assoc
        :filename filename)
      (update
        :id #(if (nil? %) filename %))))

(defmethod load-test-case
  :zen
  [filename]
  (->> filename
       slurp
       edamame.core/parse-string
       (zen/load-ns zen-ctx))
  nil)


(defn run-case [{conf :conf test-cases :test-cases :as ctx} test-case]
(let [result (run-test-case conf test-case)]
  (update ctx :test-cases #(conj % result))))

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

(defn load-cases [files]
  (let [yaml-cases (reduce (fn [cases file] (->> file
                                                 load-test-case
                                                 (conj cases))) [] files)
        zen-cases  (mapv #(zen/get-symbol zen-ctx %) (zen/get-tag zen-ctx 'stresty/case))]
    ;;TODO Proper error handling requires proper format of errors generated be zen validation
    (zen-valid? @zen-ctx)
    (concat yaml-cases zen-cases)))

(defn run [conf files]
  (let [test-cases (load-cases files)
        result     (reduce run-case {:conf conf :test-cases []} test-cases)
        sum        (sum-for-test-cases (:test-cases result))
        summary    (get-summary result)
        passed?    (zero? (:count-failed-tests summary))]

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
            :verbosity          1
            :base-url           "http://access-policy-box.aidbox.io"
            :client-id          "stresty"
            :client-secret      "stresty"
            :auth-client-id     "myapp"
            :auth-client-secret "verysecret"
            :auth-user          "patient-user"
            :auth-user-password "admin"
            })


  (def ctx* (merge ctx (auth/get-auth-headers ctx)))

  (prn (exec-step {:conf ctx*}
                  {:id    :read-from-patient
                   :desc  "Read patient from user"
                   :GET   "/Patient/pt-1"
                   :agent :user
                   :match {:status 403}}))
  
  (prn (get-auth-headers ctx* :user))
  
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
