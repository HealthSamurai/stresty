(ns stresty.reports.html
  (:require [clojure.pprint :as pp]
            [hiccup.core :as hiccup]
            [zprint.core :as zprint]
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
  [:div.rounded-l.overflow-hidden.bg-gray-600
   [:pre.p-2.text-white (with-out-str (zprint/zprint cnt))]])

(defn render-action [act]
  [:div
   [:div.flex.space-x-2.items-baseline 
    [:div.text-xs.bg-gray-100.uppercase.border.rounded.text-center.p-1 [:b (:method act)]]
    [:div.ml-1 (:url act)]]
   (when-let [b (:body act)]
     [:details
      [:summary.cursor-pointer "Body:"]
      (code-block b)])])

(defn render-step [step]
  [:details.border-solid.border-l-4
   {:class (cond
             (= :ok (:status step))
             "border-green-300"
             (= :error (:status step))
             "border-red-300"
             (= :fail (:status step))
             "border-red-300"
             :else
             "border-gray-300")
    :open (contains? #{:error :fail} (:status step))}
   [:summary.cursor-pointer.flex.items-baseline.space-x-2.px-4.py-1.mb-2
    {:class (cond
              (= :ok (:status step))
              "bg-green-50"
              (= :error (:status step))
              "bg-red-50"
              (= :fail (:status step))
              "bg-red-50")}
    [:div.text-l (or (when-let [id (:id step)] (name id)) (str "#" (:_index step)))]
    [:div (:desc step)]
    [:div.flex-1]
    [:div.text-xs
     {:class
      (cond
        (= :ok (:status step))
        "text-green-500"
        (contains? #{:error :fail} (:status step))
        "text-red-500")}
     (:status step)]]
   [:div.pl-4.space-y-2.mb-4
    (when (:do step)
      (render-action (:do step)))
    (when (:result step)
      [:details [:summary.cursor-pointer.text-xs.text-gray-500 "Result:"] (code-block (:result step))])
    (when (:error step)
      [:div.text-red-500.space-x-2 [:b "Error:"] (get-in step [:error :message])])
    (when (:match step)
      [:details [:summary.cursor-pointer.text-xs.text-gray-500 "Match:"] (code-block (:match step))])
    (when (:match-errors step)
      [:div.text-red-500
       [:div [:b "Fails:"]]
       [:ul.list-disc.px-4
        (for [e (get-in step [:match-errors])]
          [:li (pr-str e)])]])]])

(defn render-stats [stats]
  [:div.flex.space-x-2.text-xs.text-gray-600
   [:div "Passed: " [:span.text-green-700 (:passed stats)]]
   [:div "Failed: " [:span.text-red-700 (:failed stats)]]
   [:div "Errors: " [:span.text-red-700 (:errored stats)]]])

(defn render-case [case]
  [:details
   [:summary.cursor-pointer.flex.items-baseline.space-x-2.p-2.mt-1.border-solid.border-l-4.bg-gray-50
    {:class
     (if (= :fail (:status case))
       "border-red-500"
       "border-green-500")}
    [:div.text-xl (get-in case [:case :title])]
    [:div.font-xl.text-gray-700 (get-in case [:case :zen/name])]
    [:div.flex-1]
    (render-stats (:stats case))]
   [:div.space-y-1.pt-1.pb-4.ml-4
    (for [step (:steps case)]
      (render-step step))]
   ])


(defn report [data]
  [:div.p-10.px-20 {:style "width:1200px;margin:0 auto;"}
   (for [[env-nm env] data]
     [:details.my-3 {:open true}
      [:summary.flex.items-baseline.border-b.align-baseline.py-2.space-x-2.cursor-pointer
       [:div.text-2xl (or (:title env) (str env-nm))]
       [:a.text-blue-500 {:href (get-in env [:env :base-url])} (get-in env [:env :base-url])]
       [:div.flex-1]
       (render-stats (:stats env))]
      [:div
       (for [[case-nm case] (:cases env)]
         (render-case case))]])])

(defn do-report [ztx opts data]
  (hiccup/html (report-layout
                (report data))))

