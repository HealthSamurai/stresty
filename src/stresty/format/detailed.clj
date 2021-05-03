(ns stresty.format.detailed
  (:require
   [zen.core :as zen]
   [zprint.core :as zprint]
   [clojure.string :as str]))

(defn summary [ztx]
  (let [cases (zen/get-tag ztx 'sty/case)
        steps (reduce (fn [acc c] (+ acc (count (:steps (zen/get-symbol ztx c))))) 0 cases)
        res (reduce-kv
             (fn [acc _ steps]
               (->> steps vals (group-by :status)
                    (merge-with concat acc)))
             {} (:result @ztx))]

    (str "\nSummary:"
         (count cases) "cases,"
         steps "steps,"
         (count (:error res)) "error,"
         (count (:fail res)) "fail,"
         (count (:success res)) "success")))

(defn do-format
  [ztx state {tp :type ts :ts :as event}]
  #_(let [start (:start @state)
          epoch (- ts start)]
      (println epoch (:type event)))
  (let [ident (get @state :ident)]
    (cond
      (= tp 'sty/on-tests-start)
      (swap! state assoc :start ts)
      (= tp 'sty/on-env-start)
      (do
        (swap! state assoc :ident "  ")
        (println "Env:" (get-in event [:env :zen/name]) (get-in event [:env :base-url])))

      (= tp 'sty/on-env-end)
      (swap! state assoc :ident " ")


      (= tp 'sty/on-case-start)
      (do
        (swap! state assoc :ident "    ")
        (println " Case:" (or (get-in event [:case :title])
                              (get-in event [:case :zen/name]))))

      (= tp 'sty/on-case-end)
      (swap! state assoc :ident "  ")

      (= tp 'sty/on-step-start)
      (do
        (swap! state assoc :ident "  ")
        (println " -" (get-in event [:step :id]) (get-in event [:step :desc] "")))

      (= tp 'sty/on-match-ok)
      (println ident "Success")

      (= tp 'sty/on-match-fail)
      (do
        (println ident  "ERRORS:")
        (println (str ident "  ") (if (:errors event)
                                    (str/join (str "\n   " ident) (:errors event))
                                    event)))

      (= tp 'sty/on-step-error)
      (println ident tp (or (get-in event [:error :message]) (get-in event [:error]) (dissoc event :type :ts)))

      (= tp 'sty/on-step-exception)
      (println ident tp (or (get-in event [:error :message])
                            (get-in event [:error])
                            (dissoc event :type :ts)))

      (= tp 'sty/on-step-result)
      :skip #_(do
                (println ident tp)
                (println (zprint/czprint-str (:result event))))

      (= tp 'sty/on-action-result)
      (do
        (println ident tp)
        (let [s (zprint/czprint-str (:result event))
              lines (str/split s #"\n")]
          (println (->> (mapv (fn [x] (str ident "  " x)) lines)
                        (str/join "\n")))))


      (= tp 'sty/tests-summary)
      (println (summary ztx))


      (= tp 'sty.http/request)
      (println ident tp (:method event) (:url event) (dissoc event :method :url :type :ts))

      :else
      (println ident tp (dissoc event :type))

      ))
  )
