(ns stresty.reports.core
  (:require
   [stresty.reports.html]
   [cheshire.core]
   [clojure.java.io :as io]))

(defmulti do-report (fn [ztx opts data] (:report opts)))

(defn build-report [ztx opts data]
  (when-let [out  (and (:report opts) (:output opts))]
    (println "Write report to " out)
    (io/make-parents out)
    (spit out (do-report ztx opts data))))

(defmethod do-report "json"
  [ztx opts data]
  (cheshire.core/generate-string data {:pretty true}))

(defmethod do-report "html"
  [ztx opts data]
  (stresty.reports.html/do-report ztx opts data))

