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

  (-main "-p" "examples" "-f" "ndjson" "aidbox")

  (-main "-f" "stdout" "-p" "../fhir-stresty"   "aidbox")

  (-main)
  (-main "server" "--port=800")

  )

