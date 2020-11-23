(ns anti.native-select
  (:require
    [anti.util :refer [class-names block]]
    [anti.spin :refer [spin]]
    [zf.core :as zf]
    [stylo.core :refer [c c?]]
    [re-frame.core :as rf]))

(def base-class
  (c :relative
     :inline-flex
     :w-max-full
     :items-center
     :transition-all))

(def disabled-class
  (c :cursor-not-allowed))

(def chevron-class
  (c [:w 7]
     :absolute
     [:right 0]
     [:top 0]
     [:bottom 0]
     [:text-xs]
     [:text :gray-400]
     :justify-center
     :flex
     :items-center
     :pointer-events-none))

(def select-class
  (c :appearance-none
     :w-max-full
     :w-full
     [:pl 2] [:pr 7] [:py 1]
     :inline-flex
     :items-center
     [:leading-relaxed]
     :border
     :rounded
     [:bg :white]
     :transition-all
     [:focus :outline-none :shadow-outline] [:duration 200] :ease-in-out
     [:hover [:border :blue-500]]
     [:disabled [:text :gray-500] [:bg :gray-200] [:border :gray-400] :cursor-not-allowed
      [:hover [:border :gray-400]]]))

(def multiple-class
  (c [:px 2] [:py 2]
     [[:option {:padding "0.25rem 0.5rem"}]]))

(defn select
  [props & children]
  [:div {:class [base-class (class-names (:class props))
                 (when (:disabled props) disabled-class)]}
   (into [:select (merge (dissoc props :class)
                         {:class     [select-class
                                      (class-names (:select-class props))
                                      (when (:multiple props) multiple-class)]
                          :value     #?(:cljs (clj->js (or (:value props) (if (:multiple props) [] "")))
                                        :clj  (:value props))
                          :on-change (fn [e]
                                       (when-let [on-change (:on-change props)]
                                         #?(:cljs
                                            (if (:multiple props)
                                              (on-change #js {:target #js {:value (->> (.. e -target -selectedOptions)
                                                                                       (mapv #(.-value %)))}})
                                              (on-change e)))))})]
         children)
   (when-not (:multiple props)
     [:i.far.fa-chevron-down {:class chevron-class}])])

(defn zf-select
  [props & children]
  (into [select (merge (dissoc props :opts)
                       (when-let [o (:opts props)]
                         {:value     @(rf/subscribe [::zf/value o])
                          :on-change #(rf/dispatch [::zf/set-value o (.. % -target -value)])}))]
        children))

(defn demo
  []
  [block {:title "Native Select"}
   [select {}
    [:option "Option 1"]
    [:option "Option 2"]
    [:option "Option 3"]]
   [select {:disabled true}
    [:option "Option 1"]
    [:option "Option 2"]
    [:option "Option 3"]]
   [select {}
    [:option "Really long option that will likely overlap the chevron"]
    [:option "Option 2"]
    [:option "Option 3"]]
   [zf-select {:opts {:zf/root [::form] :zf/path [:zf-select]}}
    [:option {:value "option-1"} "Option 1"]
    [:option {:value "option-2"} "Option 2"]
    [:option {:value "option-3"} "Option 3"]]
   [zf-select {:opts {:zf/root [::form] :zf/path [:zf-select-multiple]} :multiple true}
    [:option {:value "option-1"} "Option 1"]
    [:option {:value "option-2"} "Option 2"]
    [:option {:value "option-3"} "Option 3"]]])
