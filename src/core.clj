(ns core
  (:require [cheshire.core :as json]
            [runner]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io])
  (:gen-class))

(def cli-options
  [["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    ;; :update-fn inc
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ["-i" "--interactive" "Interactive mode"
    :default false]
   ["-h" "--help"]
   [nil "--version" "Show version"]])

(defn -main [& args]
  (let [opts (parse-opts args cli-options)]
    (when-let [errors (:errors opts)]
      (println (first errors))
      (System/exit 1))
    (when (-> opts :options :help)
      (println "Stresty. CLI Tool for REST Tests.")
      (println "Version " (slurp (io/resource "VERSION")))
      (println "Usage: java -jar stresty.jar [arg*] file [files*]")
      (println)
      (println "Options:")
      (println (:summary opts))
      (println)
      (println "For more info see:")
      (println "https://github.com/Aidbox/stresty")
      (System/exit 0))


    (when (-> opts :options :version)
      (println "Stresty. CLI Tool for REST Tests.")
      (println "Version" (slurp (io/resource "VERSION")))
      (System/exit 0))

    (let [ctx (merge
               (:options opts)
               {:base-url (System/getenv "AIDBOX_URL")
                :client-id (System/getenv "AIDBOX_CLIENT_ID")
                :client-secret (System/getenv "AIDBOX_CLIENT_SECRET")
                :authorization-type (System/getenv "AIDBOX_AUTH_TYPE")})]
      (println "Args:" (:arguments opts))
      (println "Configuration:")
      (clojure.pprint/pprint ctx)
      (println)
      (if (:passed? (runner/run ctx (:arguments opts)))
        (System/exit 0)
        (System/exit 1)))))

(comment

  (clojure.pprint/pprint (parse-opts ["-h" "-vv" "wow/file.txt"] cli-options))


  (merge {} {:sds 42})


  (-main )

  )

