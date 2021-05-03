(ns stresty.format.default
  (:require [clojure.string :as str]))

(defn red [x]
  (str "\033[31m" x"\033[0m"))

(defn green [x]
  (str "\033[32m" x"\033[0m"))

(defn do-format
  [ztx state {tp :type ts :ts :as event}]
  (cond
    (= tp 'sty/on-tests-start)
    (swap! state assoc :start ts)

    (= tp 'sty/on-env-start)
    (println "env:" (get-in event [:env :zen/name]) (get-in event [:env :base-url]))

    (= tp 'sty/on-case-start)
    (println " case:" (get-in event [:case :zen/name]) (get-in event [:case :title] ""))

    (= tp 'sty/on-case-end)
    (println "")

    (= tp 'sty/on-step-start)
    (do (print
         (str
          " * " (let [id (get-in event [:step :id])]
                 (if (keyword? id) (name id) (str "#1")))
          " "
          (get-in event [:step :desc] "")
          ": "))
        (flush))

    (tp (set ['sty/on-step-success 'sty/on-match-ok]))
    (do
      (println (green "ok"))
      (swap! state update :passed (fn [x] (inc (or x 0)))))

    (tp (set ['sty/on-step-fail 'sty/on-match-fail]))
    (do
      ;; (println (keys event))
      (swap! state update :failed conj {:path   [(get-in event [:case :zen/name])
                                                 (get-in event [:step :id])]
                                        :errors (:errors event)})
      (println (red "fail")))

    (= tp 'sty/on-step-error)
    (do
      (swap! state update :errored conj {:path   [(get-in event [:case :zen/name])
                                                  (get-in event [:step :id])]
                                         :error  (get-in  event [:error :message]
                                                         (:error event))})
      (println (red "error"))
      (flush))

    (= tp 'sty/on-env-end)
    (let [st @state]
      (println)
      (println
       (str "Passed:" (green (:passed st)))
       (str "Failed:"  (red (count (:failed st))))
       (str "Errored:" (red (count (:errored st)))))

      (when (:errored st)
        (println "\nErrors:")
        (doseq [err (:errored st)]
          (println " in" (:path err) (:error err))))

      (when (:failed st)
        (println "\nFails:")
        (doseq [err (:failed st)]
          (println " in" (:path err) )
          (println (->> (:errors err)
                        (mapv (fn [e] (str "  * " e)))
                        (str/join "\n")))))
      (println "\n\n")
      (swap! state dissoc :passed :failed))

    )
  )
