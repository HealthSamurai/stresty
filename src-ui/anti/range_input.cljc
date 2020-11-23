(ns anti.range-input
  (:require
    [anti.util :refer [class-names block parse-int]]
    [stylo.core :refer [c c?]]
    [zf.core :as zf]
    [zframes.re-frame :as zrf]))


(def base-class
  (c :w-full
     :appearance-none
     [:h 1.5]
     [:rounded :full]
     :border
     :outline-none

     [:range-thumb :appearance-none :border-solid :border [:bg :white] [:border :gray-300] [:w 4] [:h 4] [:rounded :full]]
     [:focus [:range-thumb :shadow-outline]]
     [:pseudo ":not(:disabled)"
      [:hover [:range-thumb [:border :blue-500]]]
      [:active [:range-thumb [:border :blue-500]]]]))


(defn range-input
  [props]
  [:input (merge props
                 {:type  "range"
                  :class [base-class (class-names (:class props))]})])


(defn zf-range-input
  [props]
  [range-input
   (merge (dissoc props :opts)
          (when-let [o (:opts props)]
            (let [{:keys [min max]} @(zrf/subscribe [::zf/schema o])]
              {:value     (or @(zrf/subscribe [::zf/value o]) 0)
               :on-change #(zrf/dispatch [::zf/set-value o (parse-int (.. % -target -value))])
               :min       min
               :max       max})))])


(zrf/reg-event-db
  init-demo
  (fn [db _]
    (-> db
        (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:range :one]})
                  {:min -100 :max 100}))))


(defn demo
  []
  (zrf/dispatch [init-demo])
  (fn []
    [block {:title "Range input"}
     [range-input]
     [zf-range-input {:opts {:zf/root [::db] :zf/path [:range :one]}}]]))

