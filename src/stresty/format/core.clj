(ns stresty.format.core
  (:require [cheshire.core]
            [zprint.core :as zprint]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defmulti do-format (fn [ztx fmt state event] fmt))

(defn emit [ztx event]
  (let [ev (assoc event :ts (System/currentTimeMillis))]
    (doseq [[fmt state] (get @ztx :formatters)]
      (do-format ztx fmt state ev))))

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
      (= tp 'sty/on-zen-errors)
      (do
        (println "Syntax errors:")
        (println (str/join "\n"
                           (->>
                            (:errors event)
                            (mapv (fn [{msg :message res :resource pth :path}]
                                    (str msg " in " res " at " pth)))))))

      (= tp 'sty/on-env-start)
      (do 
        (swap! state assoc :ident "  ")
        (println "env" (get-in event [:env :zen/name]) (get-in event [:env :base-url])))

      (= tp 'sty/on-env-end)
      (swap! state assoc :ident " ")

      (= tp 'sty/on-case-start)
      (do
        (swap! state assoc :ident "    ")
        (println " case:" (get-in event [:case :zen/name])))

      (= tp 'sty/on-case-end)
      (swap! state assoc :ident "  ")

      (= tp 'sty/on-step-start)
      (do
        (swap! state assoc :ident "  ")
        (println " -" (get-in event [:step :id]) (get-in event [:step :desc] "")))

      (= tp 'sty/on-match-ok)
      (println ident "success")

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
      

      (= tp 'sty.http/request)
      (println ident tp (:method event) (:url event) (dissoc event :method :url :type :ts))

      :else
      (println ident tp (dissoc event :type))


      ))
  )

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

    (= tp 'sty/on-step-success)
    (print ".")

    (= tp 'sty/on-step-fail)
    (print "x")

    (= tp 'sty/on-step-exception)
    (print "!")

    (= tp 'sty/on-case-end)
    (print ")")

    (= tp 'sty/on-env-end)
    (print "}\n")


    )
  )

(comment

  (zprint/czprint-str
   {:body {:a 1}
    :header {:xxxx 3}})

  )
