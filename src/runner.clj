(ns runner
  (:require [clj-yaml.core :as yaml]
            [clj-http.lite.client :as http]
            [matcho]
            [pprint]
            [colors]
            [clojure.java.io :as io]))

(defn valid? [ctx script]
  true)

(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})
(defn mk-req [ctx step]
  (if-let [method (first (filter meths (keys step)))]
    (let [url (get step method)
          opts (select-keys step [:headers :auth])]
      (merge (cond->
                 {:url (str (:base-url ctx) url)
                  :throw-exceptions false
                  :headers {"content-type" "application/json"}
                  :path url
                  :method method}
               (:body step) (assoc :body (cheshire.core/generate-string (:body step)))
               (and (:client-id ctx) (:client-secret ctx))
               (assoc :basic-auth [(:client-id ctx) (:client-secret ctx)])
               ) opts))

    (println "Warn: step should contain one of methods" meths ", but" (str "\n" (yaml/generate-string step)))))


(defn run-script [ctx script]
  (println (or (:id script) (:file script)))
  (doseq [step (:steps script)]
    (when-let [req (mk-req ctx step)]
      (when (:desc step)
        (println (str (or (:desc step) "") "         \n")))
      (println (colors/yellow (colors/bold (name (:method req)))) (:path req))
      (when (:body step)
        (pprint/pretty {:ident 0} {:body (:body step)})
        (println))
      (println)
      (let [{s :status :as resp} (http/request req)
            b (when-let [b (:body resp)]
                (cheshire.core/parse-string b keyword))
            resp (cond-> {:status s} b (assoc :body b))
            errs (when-let [m (:match step)]
                   (->> (matcho/match resp m)
                        (reduce (fn [acc {pth :path exp :expected}]
                                  (assoc-in acc pth {:expected exp})
                                  ) {})))]

        (pprint/pretty {:ident 0 :path [] :errors errs} resp)
        (if (empty? errs)
          (println "\n" (colors/green "OK!"))
          #_(println "\n" (colors/red (pr-str errs)))))
      (println (colors/dark (colors/white "\n---------------------------------\n")))
      )))

(defn run-file [ctx f]
  (let [script (assoc (yaml/parse-string (slurp f)) :file f)]
    (when (valid? ctx script)
      (run-script ctx script))))

(defn run [ctx files]
  (doseq [f files]
    (println "Read " f)
    (run-file ctx f)))


(comment
  (run-file {:base-url "http://localhost:8765"} "test/sample.yaml")

  )
