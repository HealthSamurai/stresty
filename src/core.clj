(ns core
  (:require [cheshire.core :as json]
            [runner]
            [clojure.string :as str]
            [zen.core :as zen]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [stresty.web.core :as server]
            [auth])
  (:gen-class))

(defn load-edn ;; TODO: refactor loading
  [ztx filename]
  (->> filename
       slurp
       edamame.core/parse-string
       (zen/load-ns ztx)))

(def cli-options
  [["-c" "--config FILE" "Config file"
    :default "config.edn"]
   ["-f" "--file NAME" "File names with test cases"
    :default []
    :parse-fn #(str/split % #",")]
   [nil "--ui" "Start test server with UI"]
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    ;; :update-fn inc
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ["-i" "--interactive" "Interactive mode"
    :default false]
   ["-h" "--help"]
   [nil "--version" "Show version"]])

(defonce *ctx (atom {}))

(defn -main [& args]
  (let [opts (parse-opts args cli-options)]
    (when-let [errors (:errors opts)]
      (println (first errors))
      (System/exit 1))
    (when (-> opts :options :help)
      (println "Stresty. CLI Tool for REST Tests.")
      (println "Version " (slurp "VERSION"))
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

    (let [ztx        (zen/new-context)
          _          (load-edn ztx (get-in opts [:options :config]))
          config     (->> (zen/get-tag ztx 'stresty/config)
                          first
                          (zen/get-symbol ztx))
          _          (doseq [f (get-in opts [:options :file])]
                       (load-edn ztx f))
          test-cases (mapv #(zen/get-symbol ztx %)
                           (zen/get-tag ztx 'stresty/case))
          ctx        (-> (opts :options)
                         (assoc :config config)
                         (assoc :test-cases test-cases))]

      (if (:ui opts)
        (do
          (swap! *ctx assoc :ztx ztx)
          (server/start *ctx)
          (println "UI started on localhost:8080"))
        (clojure.pprint/pprint (runner/run ctx))))))

(comment
  (-main "--config" "resources/user.edn" "--file" "resources/user.edn")

  (parse-opts ["--config" "resources/user.edn" "--file" "wow/file.txt,some-file.end"] cli-options))


