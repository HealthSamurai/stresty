(ns anti.core
  (:require [re-frame.core :as rf]
            ;; [zf.core :as zf]
            [anti.style]
            [anti.select]
            [anti.radio]
            [anti.spin]
            [anti.dropdown-select]
            [anti.tags-list]
            [anti.alt-dropdown]))

(defn input [opts]
  (let [v (rf/subscribe [:zf.core/value opts])
        on-change (fn [v] (rf/dispatch [:zf.core/set-value opts v]))]
    (fn [opts]
      [:input.anti-input (merge (select-keys opts [:class :style :list])
                                {:value @v
                                 :on-change #(on-change (.. % -target -value))})])))

(defn debug-value [opts]
  [:div "Value:"
   [:pre (pr-str  @(rf/subscribe [:zf.core/value opts]))]])

(def style
  (into [] (concat anti.style/styles
                   anti.select/style
                   anti.dropdown-select/style
                   anti.radio/style)))

(def select anti.select/select)
(def select-options anti.select/select-options)
(def radio-group anti.radio/radio-group)
(def checkbox anti.radio/checkbox)
(def checkbox-group anti.radio/checkbox-group)
(def dropdown-select anti.dropdown-select/dropdown-select)
(def dropdown-datepicker anti.dropdown-select/dropdown-datepicker)
(def spin anti.spin/spin)

(def tags-list anti.tags-list/tags-list)
(def alt-dropdown anti.alt-dropdown/alt-dropdown)



(defn collection [opts]
  [:div "Collection"])
