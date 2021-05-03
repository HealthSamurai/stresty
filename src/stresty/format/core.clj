(ns stresty.format.core
  (:require [zen.core :as zen]
            [cheshire.core]
            [zprint.core :as zprint]
            [stresty.format.compact :as compact]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defmulti do-format (fn [ztx fmt state event] fmt))

(def formats {"compact"  'sty/compact-fmt
              "normal"   'sty/normal-fmt
              "detailed" 'sty/detailed-fmt
              "html"     'sty/html-fmt
              "ndjson"   'sty/ndjson-fmt
              "json"     'sty/json-fmt})

(defn set-formatters [ztx fmt-names]
  (let [fmts (->> fmt-names
                 (mapv (fn [x] (get formats x)))
                 (filter identity)
                 (reduce (fn [acc fmt] (assoc acc fmt (atom {}))) {}))
        fmts (if (empty? fmts) {'sty/normal-fmt (atom {})} fmts)]
    (swap! ztx assoc :formatters fmts)))

(defn emit [ztx event]
  (let [ev (assoc event :ts (System/currentTimeMillis))]
    (doseq [[fmt state] (get @ztx :formatters)]

      (do-format ztx fmt state ev))))

(defmethod do-format
  'sty/compact-fmt
  [ztx _ state event]
  (#'compact/do-format ztx state event))

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

(defmethod do-format
  'sty/ndjson-fmt
  [ztx _ state event]
  (println (cheshire.core/generate-string event)))

(defmethod do-format
  'sty/debug-fmt
  [ztx _ state {tp :type ts :ts :as event}]
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

      )))

(defmethod do-format
  'sty/stdout-fmt
  [ztx _ state {tp :type ts :ts :as event}]
  #_(let [start (:start @state)
          epoch (- ts start)]
      (println epoch (:type event)))
  (cond
    (= tp 'sty/on-tests-start)
    (swap! state assoc :start ts)
    (= tp 'sty/on-zen-errors)
    (do
      (println "Syntax errors:")
      (println (str/join "\n"
                         (->>
                          (:errors event)
                          (mapv (fn [{msg :message res :resource pth :path}]
                                  (str ">> " msg " in " res " at " pth)))))))

    (= tp 'sty/on-env-start)
    (print (get-in event [:env :zen/name]) "{")

    (= tp 'sty/on-case-start)
    (print "(")

    (= tp 'sty/on-step-start)
    :nop

    (tp (set ['sty/on-step-success 'sty/on-match-ok]))
    (print ".")

    (tp (set ['sty/on-step-fail 'sty/on-match-fail]))
    (print "x")

    (= tp 'sty/tests-summary)
    (summary ztx)

    (= tp 'sty/on-step-exception)
    (print "!")

    (= tp 'sty/on-case-end)
    (print ")")

    (= tp 'sty/on-env-end)
    (print "}\n")


    )
  )

(comment

  (->>
   (zprint/czprint-str
    {:body {:a 1}
     :header {:xxxx 3}})
   println)

  )
