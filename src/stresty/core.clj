(ns stresty.core
  (:require
   [zen.core :as zen]
   [stresty.matcho]
   [cheshire.core]
   [clj-http.lite.client :as http]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as str])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn current-dir []
  (System/getProperty "user.dir"))

(defn run-step [ztx suite case step-key step]
  (try
    (let [req {:method (:method step)
               :url (str (:base-url suite) (:uri step))
               :headers {"content-type" "application/json"}
               :body (when-let [b (:body step)]
                       (cheshire.core/generate-string b))}
          resp (http/request req)
          state (get-in @ztx [:state (:zen/name suite) (:zen/name case)])
          resp' (update resp :body (fn [x] (when x (cheshire.core/parse-string x keyword))))]
      (swap! ztx assoc-in [:state (:zen/name suite) (:zen/name case) step-key] resp')
      (println "   " (str (str/upper-case (name (:method req))) " " (:url req)))
      (if-let [err (:error resp)]
        (println "    >> ERROR:" (.getMessage ^Exception err))
        (if-let [matcho (:response step)]
          (let [errors (stresty.matcho/match ztx state resp' matcho)]
            (if-not (empty? errors)
              (do (println "       FAIL:" (str/join "     \n" errors))
                  (println "       RESPONSE:" (pr-str resp')))
              (if (get-in @ztx [:opts :verbose])
                (println "     ✔" (pr-str resp'))
                (println "       ✔"))))

          (println "    >>" :status (:status resp) :body (:body resp)))))
    (catch Exception e
      (println :error (.getMessage e)))))

(defn run-case [ztx suite case]
  (doseq [[k step] (->> (:steps case)
                        (sort-by (fn [[_ x]] (:row (meta x)))))]
    (println "  " (str "#" (name k))  (:desc step))
    (run-step ztx suite case k step)))

(defn eval-suite [ztx suite]
  (println "== Run suite " (:zen/name suite) (:base-url suite))
  (doseq [case-ref (zen/get-tag ztx 'sty/case)]
    (let [case (zen/get-symbol ztx case-ref)]
      (println "# case " (or (:title case) (:desc case) case-ref))
      (run-case ztx suite case))))

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "Project path"]
   ["-v" nil "Verbosity level" :id :verbose]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{opts :options args :arguments summary :summary errs :errors :as inp} (parse-opts args cli-options)]
    (cond
      (or (:help opts) (empty? args)) (do
                                        (println "sty <opts>" "SUTE_NS" "[CASE_NS]" "[STEP_NAME]")
                                        (println "opts:")
                                        (println summary))
      :else (let [pth (:path opts)
                  suite-name (when-let [nm (first args)] (symbol nm))
                  zen-opts {:paths [(if pth
                                      (if (str/starts-with? pth "/")
                                        pth
                                        (str (System/getProperty "user.dir") "/" pth))
                                      (System/getProperty "user.dir"))]}
                  ztx (zen/new-context zen-opts)]
              (swap! ztx assoc :opts opts)
              (println :read suite-name)
              (zen/read-ns ztx suite-name)
              (let [errs (:errors @ztx)]
                (when-not (empty? errs)
                  "Errors:"
                  (println (str/join "\n" errs))))
              (doseq [suite-ref (zen/get-tag ztx 'sty/suite)]
                (let [suite (zen/get-symbol ztx suite-ref)]
                  (eval-suite ztx suite)))
              )))
  )


(comment
  (-main "-p" "examples" "aidbox")

  )

;; (-main "-p" "examples"  "aidbox")
;; (parse-opts ["--path" "examples"  "aidbox"] cli-options)
