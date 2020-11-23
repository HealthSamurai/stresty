(ns anti.textarea
  (:require
   [anti.util :refer [block parse-int parse-float]]
   [anti.spin :refer [spin]]
   [stylo.core :refer [c c?]]
   [re-frame.core :as rf]
   [anti.util :refer [class-names]]
   [clojure.string :as str]
   [zf.core :as zf]))

(def base-class
  (c :inline-flex
     :w-full
     [:items-center]
     [:overflow-hidden]
     [:space-x 2]
     [:leading-relaxed]
     :border
     :rounded
     [:bg :white]
     :transition-all [:duration 200] :ease-in-out
     [:focus-within :outline-none :shadow-outline [:border :blue-500]]
     [:hover [:border :blue-500]]))

(def disabled-class
  (c [:text :gray-500]
     [:bg :gray-200]
     [:border :gray-400]
     :cursor-not-allowed
     [:hover [:text :gray-500] [:border :gray-400]]))

(def textarea-class
  (c [:py 1] [:px 2] [:leading-relaxed]
     :flex-auto
     [:w-min 0]
     [:focus :outline-none]
     [:bg :transparent]
     [:disabled :cursor-not-allowed]))

(defn textarea
  [props]
  [:div {:class [base-class
                 (when (:disabled props) disabled-class)
                 (class-names (:class props))]}
   (:prefix props)
   [:textarea (merge (dissoc props :class :textarea-class :prefix :suffix)
                     {:class [textarea-class (class-names (:textarea-class props))]})]
   (:suffix props)])

(defn zf-textarea
  [props]
  (let [o (:opts props)]
    [textarea
     (merge (dissoc props :opts)
            (when o
              {:value     (or @(rf/subscribe [::zf/value o]) "")
               :id        (zf/get-id o)
               :on-change #(rf/dispatch [::zf/set-value o (.. % -target -value) {:auto-commit false}])
               :on-blur   #(rf/dispatch [::zf/commit-value o])}))]))


(defn demo
  []
  [block {:title "Textareas"}
   [textarea {:placeholder "Default"}]
   [textarea {:placeholder "Default" :default-value "With value"}]
   [textarea {:placeholder "Disabled" :disabled true}]
   [textarea {:placeholder "Disabled" :disabled true :default-value "With value"}]
   [textarea {:placeholder "With prefix" :class (c [:w 40]) :prefix [:i.far.fa-search {:class (c [:text :gray-500])}]}]
   [textarea {:placeholder "With suffix" :class (c [:w 40]) :suffix [:i.far.fa-eye {:class (c [:text :gray-500])}]}]])
