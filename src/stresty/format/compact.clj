(ns stresty.format.compact
  (:require [clojure.string :as str]))

(defn do-format
  [ztx state {tp :type ts :ts :as event}]
  (cond
    (= tp 'sty/on-tests-start)
    (swap! state assoc :start ts)

    (= tp 'sty/on-env-start)
    (do (print "#" (get-in event [:env :zen/name]))
        (flush))

    (= tp 'sty/on-case-start)
    (do (print "(") (flush))

    (= tp 'sty/on-step-start)
    :nop

    (tp (set ['sty/on-step-success 'sty/on-match-ok]))
    (do
      (print ".")
      (flush)
      (swap! state update :passed (fn [x] (inc (or x 0)))))

    (tp (set ['sty/on-step-fail 'sty/on-match-fail]))
    (do
      ;; (println (keys event))
      (swap! state update :failed conj {:path   [(get-in event [:case :zen/name])
                                                 (get-in event [:step :id])]
                                        :errors (:errors event)})
      (print "x")
      (flush))

    (= tp 'sty/on-step-error)
    (do
      (swap! state update :errored conj {:path   [(get-in event [:case :zen/name])
                                                  (get-in event [:step :id])]
                                         :error  (get-in  event [:error :message]
                                                         (:error event))})
      (print "!")
      (flush))

    (= tp 'sty/on-case-end)
    (do (print ")") (flush))

    (= tp 'sty/on-env-end)
    (let [st @state]
      (println)
      (println
       (str "Passed:" (:passed st))
       (str "Failed:"  (count (:failed st)))
       (str "Errored:" (count (:errored st))))

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


    ;; (= tp 'sty/tests-summary)
    ;; (let [st @state]
    ;;   (println "Passed:" (:passed st)
    ;;            "Failed:" (count (:failed st))))
    )
  )
