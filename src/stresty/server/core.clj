(ns stresty.server.core
  (:require [zen.core :as zen]
   [stresty.actions.core]
   [stresty.matchers.core]
   [stresty.server.core]
   [stresty.server.http]
   [stresty.server.cli :as cli]
   [stresty.operations.core]
   [stresty.format.core :as fmt]
   [stresty.format.report]
   [stresty.sci]
   [clojure.string :as str]))


(defmulti command
  (fn [ztx cmd] (:name cmd)))

(defmethod command 'sty/cli-server
  [ztx cmd]
  (stresty.server.http/start-server ztx (update (:params cmd) :port (fn [x] (when x (Integer/parseInt x))))))

(defmethod command 'sty/cli-test
  [ztx cmd]
  (stresty.operations.core/op ztx {:method 'sty/run-tests
                                   :params (or (:params cmd) {})}))

(defmethod command 'sty/cli-gen
  [ztx cmd]
  (stresty.operations.core/op ztx {:method 'sty/gen
                                   :params (or (:params cmd) {})}))

(defmethod command 'sty/cli-check
  [ztx cmd])

(defmethod command 'sty/cli-help
  [ztx cmd]
  (println (cli/usage ztx)))

(defmethod command :default
  [ztx cmd]
  (println "ERROR: command "  (:name cmd) " is not implemented.")
  {:error {:message (str "ERROR: command "  (:name cmd) " is not implemented.")}})

(defn current-dir []
  (System/getProperty "user.dir"))

(defn calculate-paths [pth]
  [(if pth
     (if (str/starts-with? pth "/")
       pth
       (str (System/getProperty "user.dir") "/" pth))
     (System/getProperty "user.dir"))])

(defn report-zen-errors [ztx]
  (let [errs (:errors @ztx)]
    (when-not (empty? errs)
      (println "Syntax errors:")
      (println (str/join "\n"
                         (->> errs
                              (mapv (fn [{msg :message res :resource pth :path}]
                                      (str msg " in " res " at " pth)))))))))

(defn configure-format [ztx opts]
  (swap! ztx assoc :opts opts :formatters
         (let [fmt (get {"ndjson" 'sty/ndjson-fmt
                         "stdout" 'sty/stdout-fmt
                         "report" 'sty/report-fmt ;; html report
                         "debug"  'sty/debug-fmt}
                        (:format opts)
                        'sty/debug-fmt)]
           {fmt (atom {})})))

(defn start-server [{opts :params}]
  (let [paths (calculate-paths (:path opts))
        ztx (zen/new-context {:opts opts :paths paths})]
    (zen/read-ns ztx 'sty)
    (if-let [ns (:ns opts)]
      (zen/read-ns ztx (symbol ns))
      (println "WARN: No entry point provided."))
    (report-zen-errors ztx)
    ztx))

(defn stop-server [ztx]
  (stresty.server.http/stop-server ztx))

(defn exec [{cmd-params :command :as args}]
  (let [ztx (start-server args)]
    (if-not cmd-params
      (do
        (println "WARN: No command provided")
        (println (cli/usage ztx))
        {:error {:message "No command provided"}})
      (let [{err :error cmd :result :as resp} (cli/resolve-cmd ztx cmd-params)]
        (if err
          (do
            (println (or (:message err) "Error"))
            (println (str/join "\n * " (:errors err)))
            resp)
          (do
            (println "cmd:" (:name cmd))
            (command ztx cmd)))))))

(defn main [args]
  (exec (cli/parse-args args)))

(comment
  (def ztx (start-server {}))
  (stop-server ztx)

  (def p (main [ "--path=/Users/aitem/Work/HS/ssty/t" "--format=report" "--output=output" "--ns=rmis" "test"]))
  (def p (main [ "--path=/Users/aitem/Work/HS/stresty-fhir" "--format=report" "--output=output" "--ns=servers" "test"]))

  )
