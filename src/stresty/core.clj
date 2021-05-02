(ns stresty.core
  (:require [stresty.server.core :as server]
            [clojure.string :as str])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn parse-args [args]
  (loop [[a & as] args
         params {}]
    (if (and (nil? a) (empty? as))
      params
      (if (str/starts-with? a "--")
        (let [[k v] (str/split a #"=" 2)]
          (recur as (assoc-in params [:params (keyword (str/replace k #"^--" ""))] (str/trim v))))
        (assoc params :command (merge {:name (str/trim a)} (parse-args as)))))))


(defn -main [& args]
  (stresty.server.core/exec (parse-args args)))


(comment
  (parse-args ["--path=PATH" "tests" "--parama=1" "--paramb=2"])
  (parse-args [])
  (parse-args ["server" "--port=800"])



  (-main "-f" "stdout" "-p" "../fhir-stresty"   "aidbox")


  (def marata-test
    (do
      ;;(prn "---------------------------------------------")
      ;;(-main "--path=/Users/aitem/Work/HS/ssty/t" "--ns=rmis" "tests")
      (prn "---------------------------------------------")
      (def p (-main "--path=/Users/aitem/Work/HS/ssty/t" "--env=rmis/main" "--format=report" "--ns=rmis" "tests"))
      (prn "---------------------------------------------")
      (def p (-main "--path=/Users/aitem/Work/HS/ssty/t" "--format=report" "--output=output" "--ns=rmis" "tests"))
      ))

  (def ctx (-main "server" "--port=8888"))
  (type ctx)

  (stresty.server.core/stop-server ctx)

  (def res (-main "--path=examples" "--ns=aidbox" "--format=debug" "tests"))

  (-main "--path=.tmp" "gen" "--project=my.super.project")

  )
