(ns stresty.server.core
  (:require
   [zen.core :as zen]
   [stresty.format.core]
   [stresty.actions.core]
   [stresty.matchers.core]
   [stresty.server.core]
   [stresty.server.http]
   [stresty.server.cli]
   [stresty.sci]
   [clojure.string :as str]))

(defn start-server [opts]
  (let [ztx (zen/new-context {})]
    (zen/read-ns ztx 'sty)))

(defn stop-server [ztx])

(defmulti command
  (fn [ztx cmd] (:name cmd)))

(defmethod command "server"
  [ztx cmd]
  (println cmd))

(defmethod command "tests"
  [ztx cmd]
  (println cmd))

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

(defn exec [{cmd :command opts :params}]
  (let [ztx (zen/new-context {:opts opts :paths (calculate-paths (:path opts))})]
    (zen/read-ns ztx 'sty)
    (command ztx cmd)))


(comment
  (def ztx (zen/new-context {}))
  (zen/read-ns ztx 'sty)

  (start-server ztx {})
  (stop-server ztx)

  )
