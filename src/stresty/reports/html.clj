(ns stresty.reports.html
  (:require [clojure.pprint :as pp]
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

(defn code-block [cnt]
  [:div.mt-3.rounded-l.overflow-hidden.bg-gray-800
   [:pre.p-2.text-white (with-out-str (pp/pprint cnt))]])

#_(defn summary [ztx state ts]
  (let [cases (zen/get-tag ztx 'sty/case)
        steps (reduce (fn [acc c] (+ acc (count (:steps (zen/get-symbol ztx c))))) 0 cases)
        res (reduce-kv
             (fn [acc _ steps]
               (->> steps vals (group-by :status)
                    (merge-with concat acc)))
             {} (:result @ztx))]

    [:div [:b "Summary: "]
     (str (count cases) " cases, "
          steps " steps, "
          (count (:error res)) " errors, "
          (count (:fail res)) " fails, "
          (count (:success res)) " success "
          "(Total " (- ts (:start @state) ) " ms)")]))

#_(defmethod fmt/do-format
  'sty/report-fmt
  [ztx _ state {tp :type ts :ts :as event}]
  (let [b #(swap! state update :body conj  %)
        c #(swap! state update :case conj  %)
        s #(swap! state update :step conj  %)]
    (when-not (:body @state)
      (swap! state assoc :body [:div.container.mx-auto]))
    (cond
      (= tp 'sty/on-tests-start)
      (do (swap! state assoc :start ts))

      (= tp 'sty/on-zen-errors)
      (do
        (println "Syntax errors:")
        (println (str/join "\n"
                           (->>
                            (:errors event)
                            (mapv (fn [{msg :message res :resource pth :path}]
                                    (str ">> " msg " in " res " at " pth)))))))

      (= tp 'sty/on-env-start)
      (b [:div.my-6.p-3.rounded.bg-gray-100
          [:dl.grid {:style "grid-template-columns: 140px 1fr"}
           [:dt.text-right.mr-2 "Environment:"]
           [:dd (get-in event [:env :zen/name])]

           [:dt.text-right.mr-2 [:img.w-4.mx-2.inline-block
                 {:src "https://cdn.jsdelivr.net/npm/heroicons@1.0.1/outline/globe-alt.svg"}]]
           [:dd [:a.text-blue-500 {:href (get-in event [:env :base-url])} (get-in event [:env :base-url])]]

           [:dt.text-right.mr-2 "Authorization:"]
           [:dd "Basic Auth"]

           [:dt.text-right.mr-2 "ClientID:"]
           [:dd (get-in event [:env :basic-auth :user])]

           [:dt.text-right.mr-2 "Passoword:"]
           [:dd "*********"]]])

      (= tp 'sty/on-case-start)
      (do (swap! state assoc :case [:div.bg-gray-50.border-solid.border-l-4.border-light-blue-500.p-3.mt-2])
          (b  [:div.mt-6.text-xl
               [:span.font-medium (or (get-in event [:case :title]) (get-in event [:case :zen/name]))]
               [:span.text-l.ml-3 (:zen/file (zen/get-symbol ztx (get-in event [:case :zen/name]) ))]] ))

      (= tp 'sty/on-case-end)
      (b (:case @state))

      (= tp 'sty/on-step-start)
      (do (swap! state assoc :step [:div.bg-green-200.p-2.mb-4.roun-l])
          (swap! state assoc :step-ts ts)
          (c [:div.text-xl
              (or (get-in event [:step :desc])
                  (str "ID: "(get-in event [:step :id])))]))

      (= tp 'sty/on-step-end)
      (do (swap! state update-in [:step 1 1] conj [:div.float-right ( str  (- ts (:step-ts @state)) " ms")])
        (c (:step @state)))

      (= tp 'sty.http/request)
      (let [source-event (:source event)]
        (s [:div
            [:div.req
             [:img.w-4.mr-2.inline-block
              {:src "https://cdn.jsdelivr.net/npm/heroicons@1.0.1/outline/globe-alt.svg"}]

             [:div.inline-block.uppercase.bg-white.w-20.rounded.text-center
              (:method event)]
             [:span.ml-2 [:a.text-blue-500 {:href (:url event) :target "_blank"} (:url source-event)]]]
            (when (:body source-event)
              (code-block (:body source-event)))]))

      ;;(tp (set ['sty/on-step-success 'sty/on-match-ok]))
      ;;(b [:div [:span.bg-green-500  "success"]])

      (tp (set ['sty/on-step-fail 'sty/on-match-fail]))
      (do (swap! state assoc-in [:step 0] :div.bg-red-200.p-2.mt-1.rounded-l.mb-4)
          (s [:div
              [:div.mt-3 [:b "Fail: assertion fail"]
               (code-block (if (:errors event) (:errors event) event))]
              [:div.mt-3 [:b "Response"]
               (code-block (if (:result event) (:result event) event))]]))

      (= tp 'sty/on-tests-done)
      (b [:div.my-6.p-3.rounded.bg-gray-100  (summary ztx state ts)])

      (= tp 'sty/on-step-exception)
      (b [:div [:span.bg-red-500  "STEP fail"]])



      (= tp 'sty/on-env-end)
      (println "DONE ENV")


      (= tp 'sty/on-tests-done)
      (let [file "output/index.html"]
        (io/make-parents file)
        (spit file (hiccup/html  (report-layout (:body @state))))
        (println "DONE TEST")))))

(defn do-report [ztx opts data]
  (hiccup/html (report-layout [:h1 "Report"])))

