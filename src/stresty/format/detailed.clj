(ns stresty.format.detailed
  (:require
   [zen.core :as zen]
   [zprint.core :as zprint]
   [clojure.string :as str]))


(defn red [x]
  (str "\033[31m" x"\033[0m"))

(defn green [x]
  (str "\033[32m" x"\033[0m"))

(defn pretty [ident x]
  (let [s (zprint/czprint-str x)
        lines (str/split s #"\n")]
    (println (->> (mapv (fn [x] (str ident x)) lines)
                  (str/join "\n")))))

(defn summary [ztx]
  (let [cases (zen/get-tag ztx 'sty/case)
        steps (reduce (fn [acc c] (+ acc (count (:steps (zen/get-symbol ztx c))))) 0 cases)
        res (reduce-kv
             (fn [acc _ steps]
               (->> steps vals (group-by :status)
                    (merge-with concat acc)))
             {} (:result @ztx))]

    (str "\nSummary: "
         (count cases) " cases, "
         steps " steps, "
         (count (:error res)) " errored, "
         (count (:fail res)) " failed, "
         (count (:success res)) " passed ")))

(defn do-format
  [ztx state {tp :type ts :ts :as event}]
  (cond
    (= tp 'sty/on-tests-start)
    (swap! state assoc :start ts)

    (= tp 'sty/on-env-start)
    (do
      (swap! state assoc :ident "  ")
      (println "env:" (get-in event [:env :zen/name]) (get-in event [:env :base-url])))

    (= tp 'sty/on-env-end)
    (swap! state assoc :ident " ")


    (= tp 'sty/on-case-start)
    (do
      (swap! state assoc :ident "    ")
      (println "case:" (get-in event [:case :zen/name]) (get-in event [:case :title] "")))

    (= tp 'sty/on-case-end)
    (swap! state assoc :ident "  ")

    (= tp 'sty/on-step-start)
    
    (println
     (str
      "* " (let [id (get-in event [:step :id])]
             (if (keyword? id) (name id) (str "#1")))
      " "
      (get-in event [:step :desc] "")))

    (= tp 'sty/on-run-step)
    :nop

    (= tp 'sty/on-match-ok)
    (println  (green "  Success!"))

    (= tp 'sty/on-match-fail)
    (do
      (println  (red "  Fail:"))
      (when-let [err (:errors event)]
        (println 
         (->> err
              (mapv #(str "  - " %))
              (str/join "\n")))))


    (= tp 'sty/on-step-error)
    (do
      (println  (red "  Error:") (get-in event [:error :message])))

    (= tp 'sty/on-action-result)

    (when (:result event)
      (println "  Response:")
      (pretty  "  " (:result event)))


    (= tp 'sty/on-tests-done)
    (println (summary ztx))


    (= tp 'sty.http/request)
    (do 
      (println " " (when-let [m (:method event)] (str/upper-case (name m))) (:url event))
      (when-let [b (get-in event [:source :body])]
        (pretty "  " b)
        ))

    ;; :else
    ;; (println ident tp (dissoc event :type))

    )
  )
