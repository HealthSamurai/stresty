(ns stresty.server.core
  (:require [zen.core :as zen]
   [stresty.actions.core]
   [stresty.matchers.core]
   [stresty.server.core]
   [stresty.server.http]
   [stresty.server.cli]
   [stresty.operations.core]
   [stresty.format.core :as fmt]
   [stresty.format.report]
   [stresty.sci]
   [clojure.string :as str]))


(defmulti command
  (fn [ztx cmd] (get-in cmd [:command :name])))

(defmethod command "server"
  [ztx cmd]
  (stresty.server.http/start-server ztx (update (:params cmd) :port (fn [x] (when x (Integer/parseInt x))))))

(defmethod command "tests"
  [ztx cmd]
  (stresty.operations.core/op ztx {:method 'sty/run-tests
                                   :params (or (:params cmd) {})}))

(defmethod command "gen"
  [ztx cmd]
  (stresty.operations.core/op ztx {:method 'sty/gen
                                   :params (or (:params cmd) {})}))

(defmethod command :default
  [ztx cmd]
  (println "
Usage:

sty --path=PATH <command> <subcommand>

sty server -p PORT

sty tests --envs=ENV1,ENV2 --cases=CS,CS  --steps=STEP,STEP --tags=TAG,TAG

sty watch --paths=PATH1,PATH2

sty => help

"))

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
      (fmt/emit ztx {:type 'sty/on-zen-errors :errors errs}))))

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
  (let [ztx (zen/new-context {:opts opts :paths (calculate-paths (:path opts))})]
    (zen/read-ns ztx 'sty)
    (when-let [ns (:ns opts)]
      (zen/read-ns ztx (symbol ns)))
    (configure-format ztx opts)
    (report-zen-errors ztx)
    ztx))

(defn stop-server [ztx]
  (stresty.server.http/stop-server ztx))

(defn exec [{cmd :command :as args}]
  (let [ztx (start-server args)]
    (command ztx args)
    ztx))

(comment
  (def ztx (start-server {}))
  (stop-server ztx)

  )
