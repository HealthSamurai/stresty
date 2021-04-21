(ns stresty.core
  (:require
   [zen.core :as zen]
   [cheshire.core]
   [clj-http.lite.client :as http]
   [clojure.string :as str])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn current-dir []
  (System/getProperty "user.dir"))


(defn -main [& args]
  (let [^String pth (first args)
        ^String ns (second args)
        opts {:paths [(str (System/getProperty "user.dir") "/" pth)]}
        ztx (zen/new-context opts)]
    (println :opts opts)
    (println :read (symbol ns))
    (zen/read-ns ztx (symbol ns))
    (let [errs (:errors @ztx)]
      (when-not (empty? errs)
        (println (str/join "\n" errs))))
    (doseq [suite-ref (zen/get-tag ztx 'sty/suite)]
      (let [suite (zen/get-symbol ztx suite-ref)]
        (println "== Run suite " (:zen/name suite) (:base-url suite))
        (doseq [case-ref (zen/get-tag ztx 'sty/case)]
          (let [case (zen/get-symbol ztx case-ref)]
            (println "# case " (or (:title case) (:desc case) case-ref))
            (doseq [[k step] (->> (:steps case)
                                  (sort-by (fn [[_ x]] (:row (meta x)))))]
              (println "  *" (or (:desc step) (name k)))
              ;; (println (pr-str step))
              (try 
                (let [req {:method (:method step)
                           :url (str (:base-url suite) (:uri step))
                           :headers {"content-type" "application/json"}
                           :body (when-let [b (:body step)]
                                   (cheshire.core/generate-string b))}
                      resp (http/request req)]
                  (println "   " (str (str/upper-case (name (:method req))) " " (:url req)))
                  (if-let [err (:error resp)]
                    (println "    >> ERROR:" (.getMessage ^Exception err))
                    (println "    >>" :status (:status resp) :body (:body resp))))
                (catch Exception e
                  (println :error (.getMessage e)))))
            ))))))


(comment
  (-main "examples" "aidbox")


  )
