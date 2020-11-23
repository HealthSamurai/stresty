(ns anti.calendar
  (:require
    [chrono.calendar :as cal]
    [stylo.core :refer [c c?]]
    [anti.util :refer [block]]))

(def date-class
  (c [:w 7] [:h 7]
     :flex
     :justify-center
     :items-center
     :leading-none
     :rounded
     :relative
     :transition-all
     [:focus :border :outline-none :shadow-outline] [:duration 200] :ease-in-out
     [:hover :border [:text :blue-500] [:border :blue-500]]
     [:active :border [:text :blue-600] [:border :blue-600]]))

(defn calendar
  [props]
  (let [calendar (:cal (cal/for-month (:year props) (:month props)))]
    [:table {:class (c)}
     [:tbody
      [:tr
       (for [[_ {day :name}] cal/weeks]
         [:td {:key day}
          [:div {:class (c [:w 7] [:h 7] :flex :justify-center :items-center :leading-none)}
           (subs day 0 2)]])]
      (for [[index week] (map-indexed vector calendar)]
        [:tr {:key index}
         (for [[index date] (map-indexed vector week)]
           [:td {:key index}
            [:button
             {:class [date-class
                      (when-not (:current date) (c [:text :gray-500]))]
              :type  "button"}
             (:day date)]])])]]))


(defn demo
  []
  [block {:title "Calendars" :width "16rem"}
   [calendar {:year 2020 :month 7}]
   [calendar {:year 2020 :month 8}]])
