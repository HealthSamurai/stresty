(ns stresty.format.report
  (:require [stresty.format.core :as fmt]
            [clojure.string :as str]))

(defmethod fmt/do-format
  'sty/report-fmt
  [ztx _ state {tp :type ts :ts :as event}]
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

    ;;(= tp 'sty/tests-summary)
    ;;(summary ztx)

    (= tp 'sty/on-step-exception)
    (print "!")

    (= tp 'sty/on-case-end)
    (print ")")

    (= tp 'sty/on-env-end)
    (print "}\n")


    (= tp 'sty/tests-done)
    (print "done \n")

    )

  )
