(ns stresty.core
  (:require
   [zen.core :as zen]
   [stresty.matcho]
   [stresty.format.core :as fmt]
   [stresty.sci]
   [stresty.server]
   [cheshire.core]
   [clj-http.lite.client :as http]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as str])
  (:gen-class))


(set! *warn-on-reflection* true)

(defn current-dir []
  (System/getProperty "user.dir"))

(defn run-step [ztx suite case step-key step]
  (let [state (or (get-in @ztx [:state (:zen/name suite) (:zen/name case)]) {})
        step (stresty.sci/eval-data {:namespaces {'sty {'step step 'case case 'state state}}} step)
        req {:method (:method step)
             :url (str (:base-url suite) (:uri step))
             :headers {"content-type" "application/json"}
             :body (when-let [b (:body step)]
                     (cheshire.core/generate-string b))}
        ev-base {:type 'sty/on-step-start :suite suite :case case
                 :step (assoc step :id step-key)
                 :verbose (get-in @ztx [:opts :verbose])
                 :request req}]
    (fmt/emit ztx ev-base)
    (try
      (let [resp (-> (http/request req)
                     (update :body (fn [x] (when x (cheshire.core/parse-string x keyword)))))]
        (swap! ztx
               (fn [old]
                 (update-in old [:state (:zen/name suite) (:zen/name case)]
                            (fn [state] (assoc state step-key resp)))))

        (if-let [err (:error resp)]
          (fmt/emit ztx (assoc ev-base :type 'sty/on-step-exception :exception err))
          (if-let [matcho (:response step)]
            (let [errors (stresty.matcho/match ztx state resp matcho)]
              (if-not (empty? errors)
                (fmt/emit ztx (assoc ev-base :type 'sty/on-step-fail :errors errors :response resp))
                (fmt/emit ztx (assoc ev-base :type 'sty/on-step-success :response resp))))
            (fmt/emit ztx (assoc ev-base :type 'sty/on-step-response :response resp)))))
      (catch Exception e
        (fmt/emit ztx (assoc ev-base :type 'sty/on-step-exception :exception e))))))

(defn run-case [ztx suite case]
  (fmt/emit ztx {:type 'sty/on-case-start :suite suite :case case})
  (doseq [[k step] (->> (:steps case)
                        (sort-by (fn [[_ x]] (:row (meta x)))))]
    (run-step ztx suite case k step))
  (fmt/emit ztx {:type 'sty/on-case-end :suite suite :case case}))

(defn eval-suite [ztx suite]
  (fmt/emit ztx {:type 'sty/on-suite-start :suite suite})
  (doseq [case-ref (zen/get-tag ztx 'sty/case)]
    (let [case (zen/get-symbol ztx case-ref)]
      (run-case ztx suite case)))
  (fmt/emit ztx {:type 'sty/on-suite-end :suite suite}))

(def cli-options
  ;; An option with a required argument
  [["-p" "--path PATH" "Project path"]
   ["-f" "--format FORMAT" "Report format can be ndjson, debug, html"]
   ["-v" nil "Verbosity level" :id :verbose]
   ["-h" "--help"]])

(defn calculate-paths [pth]
  [(if pth
     (if (str/starts-with? pth "/")
       pth
       (str (System/getProperty "user.dir") "/" pth))
     (System/getProperty "user.dir"))])

(defn -main [& args]
  (let [ztx (zen/new-context {})]
    (stresty.server/start-server ztx {}))
  #_(let [{opts :options args :arguments summary :summary errs :errors :as inp} (parse-opts args cli-options)]
    (cond
      (or (:help opts) (empty? args)) (do
                                        (println "sty <opts>" "SUTE_NS" "[CASE_NS]" "[STEP_NAME]")
                                        (println "opts:")
                                        (println summary))
      :else (let [pth (:path opts)
                  suite-name (when-let [nm (first args)] (symbol nm))
                  zen-opts {:paths (calculate-paths pth)}
                  ztx (zen/new-context zen-opts)]
              (swap! ztx assoc :opts opts :formatters
                     (let [fmt (get {"ndjson" 'sty/ndjson-fmt
                                     "stdout" 'sty/stdout-fmt
                                     "debug"  'sty/debug-fmt}
                                       (:format opts)
                                       'sty/stdout-fmt)]
                             {fmt (atom {})}))
              (fmt/emit ztx {:type 'sty/on-tests-start :entry-point suite-name :opts zen-opts})
              ;; TBD: fail on unexistiong suite
              (zen/read-ns ztx suite-name)
              (let [errs (:errors @ztx)]
                (when-not (empty? errs)
                  (fmt/emit ztx {:type 'sty/on-zen-errors :errors errs})))
              (doseq [suite-ref (zen/get-tag ztx 'sty/suite)]
                (let [suite (zen/get-symbol ztx suite-ref)]
                  (eval-suite ztx suite)))
              (fmt/emit ztx {:type 'sty/on-tests-end :entry-point suite-name})))))


(comment
  (-main "-p" "examples" "-f" "ndjson" "aidbox")

  (-main "-f" "stdout" "-p" "../fhir-stresty"   "aidbox")

  )

