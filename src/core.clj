(ns core
  (:require [cheshire.core :as json]
            [runner]
            [clojure.string :as str]
            [zen.core :as zen]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [auth])
  (:gen-class))

(defn load-edn ;; TODO: refactor loading
  [ztx filename]
  (->> filename
       slurp
       edamame.core/parse-string
       (zen/load-ns ztx)))

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


    (let [ztx (zen/new-context)
          _ (load-edn ztx (get-in opts [:arguments 0]))
          config (->> (zen/get-tag ztx 'stresty/config)
                     first
                     (zen/get-symbol ztx))
          ctx (-> (opts :options)
                  (assoc :config config)
                  (assoc :ztx ztx)
                  )]
      (prn "config:" config)
      (println "Args:" (:arguments opts))
      (println "Configuration:")
      (println)
      (if (:passed? (runner/run ctx (:arguments opts)))
        (System/exit 0)
        (System/exit 1)))))

(comment
  (-main ["resources/user.edn"])

  (parse-opts ["-h" "-vv" "wow/file.txt some-file.end"] cli-options)

  )
