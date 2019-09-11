(ns core
  (:require [cheshire.core :as json]
            [runner]
            [clojure.string :as str])
  (:gen-class))

(defn -main [& args]
  (let [ctx {:base-url (System/getenv "AIDBOX_URL")
             :basic-auth (System/getenv "AIDBOX_BASIC_AUTH")
             :verbosity 2}]
    (println "Args:" args)
    (if (:failed (runner/run ctx args))
      (System/exit 1))
    ))

(comment

  (-main )

  )

