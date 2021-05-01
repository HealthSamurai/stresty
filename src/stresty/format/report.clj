(ns stresty.format.report
  (:require [stresty.format.core :as fmt]
            [hiccup.core :as hiccup]
            [clojure.string :as str]))

(defn report-layout [body]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:content "width=device-width, initial-scale=1, shrink-to-fit=no" :name "viewport"}]
    [:meta {:content "Stresty test report", :name "description"}]
    [:meta {:content "HealthSamurai", :name "author"}]
    [:title "Stresty test report"]
    [:link  {:href "https://unpkg.com/tailwindcss@^2/dist/tailwind.min.css" :rel "stylesheet"}]]
   [:body body]])

(defmethod fmt/do-format
  'sty/report-fmt
  [ztx _ state {tp :type ts :ts :as event}]
  (let [b #(swap! state update :body conj  %)]
    (when-not (:body @state)
      (swap! state assoc :body [:div.content]))
    (cond
      (= tp 'sty/on-tests-start)
      (do (swap! state assoc :start ts)
          (b  [:div [:b "Start"]]))

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
      (b [:div [:h2 (or (get-in event [:case :title])
                        (get-in event [:case :zen/name]))]] )



      (= tp 'sty.http/request)
      (b [:div [:b (str tp (:method event) (:url event) (dissoc event :method :url :type :ts))]])


      (= tp 'sty/on-step-start)
      :nop

      (tp (set ['sty/on-step-success 'sty/on-match-ok]))
      (print ".")

      (tp (set ['sty/on-step-fail 'sty/on-match-fail]))
      (print "x")

      (= tp 'sty/tests-summary)
      [:div  (fmt/summary ztx)]

      (= tp 'sty/on-step-exception)
      (print "!")

      (= tp 'sty/on-case-end)
      (print ")")

      (= tp 'sty/on-env-end)
      (print "}\n")


      (= tp 'sty/tests-done)
      (do (b  [:div [:b "End"]])
          (spit "output/index.html" (hiccup/html  (report-layout (:body @state)))))

      ))

  )
