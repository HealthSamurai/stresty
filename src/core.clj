(ns core
  (:require [cheshire.core :as json]
            [runner]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  [["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    ;; :update-fn inc
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ["-h" "--help"]])

(defn -main [& args]
  (let [opts (parse-opts args cli-options)]
    (when-let [errors (:errors opts)]
      (println (first errors))
      (System/exit 1))
    (when (-> opts :options :help)
      (println "Stresty. CLI Tool for REST Tests.")
      (println "Usage: java -jar stresty.jar [arg*] file [files*]")
      (println)
      (println "Options:")
      (println (:summary opts))
      (println)
      (println "For more info see:")
      (println "https://github.com/Aidbox/stresty")
      (System/exit 0))
    (let [ctx (merge
               (:options opts)
               {:base-url (System/getenv "AIDBOX_URL")
                :basic-auth (System/getenv "AIDBOX_BASIC_AUTH")})]
      (if (:failed (runner/run ctx (:arguments opts)))
        (System/exit 1))))

  #_(let [ctx {:base-url (System/getenv "AIDBOX_URL")
             :basic-auth (System/getenv "AIDBOX_BASIC_AUTH")
             :verbosity 2}]

    (clojure.pprint/pprint (parse-opts args cli-options))
    (println "Args:" args)

    (if (:failed (runner/run ctx args))
      (System/exit 1))
    ))

(comment

  (clojure.pprint/pprint (parse-opts ["-h" "-vv" "wow/file.txt"] cli-options))


  (merge {} {:sds 42})


  (-main )

  )

