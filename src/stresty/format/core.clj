(ns stresty.format.core
  (:require [cheshire.core]
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
  (cond
    (= tp 'sty/on-tests-start)
    (swap! state assoc :start ts)
    (= tp 'sty/on-zen-errors)
    (do
      (println "Syntax errors")
      (println (str/join "\n"
                         (->>
                          (:errors event)
                          (mapv (fn [{msg :message res :resource pth :path}]
                                  (str msg " in " res " at " pth)))))))

    (= tp 'sty/on-env-start)
    (println "==" (get-in event [:env :zen/name])
             (get-in event [:env :base-url]))

    (= tp 'sty/on-case-start)
    (println " #" (get-in event [:case :zen/name]))

    (= tp 'sty/on-step-start)
    (print "  *" (name (get-in event [:step :id])) "=>"
           (str "TBD: render action"))

    (= tp 'sty/on-step-success)
    (println " success")

    (= tp 'sty/on-step-fail)
    (do 
      (println " fail")
      (println "   " (str/join "\n    " (:errors event))))

    (= tp 'sty/on-step-exception)
    (println " exception " (pr-str (:exception event)))

    :else
    (println " ??" tp event)


    )
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
