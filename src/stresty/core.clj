(ns stresty.core
  (:require
   [zen.core :as zen]
   [stresty.format.core]
   [stresty.actions.core]
   [stresty.matchers.core]
   [stresty.server.core]
   [stresty.sci]
   [cheshire.core]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as str])
  (:gen-class))


(set! *warn-on-reflection* true)

(defn current-dir []
  (System/getProperty "user.dir"))

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
    (zen/read-ns ztx 'sty)
    (stresty.server.core/start-server ztx {})))


(comment
  (-main "-p" "examples" "-f" "ndjson" "aidbox")

  (-main "-f" "stdout" "-p" "../fhir-stresty"   "aidbox")

  )

