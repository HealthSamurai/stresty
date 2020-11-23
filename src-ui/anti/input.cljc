(ns anti.input
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
     [:px 2]
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

(def input-class
  (c [:py 1] [:leading-relaxed]
     :flex-auto
     [:w-min 0]
     [:focus :outline-none]
     [:bg :transparent]
     [:disabled :cursor-not-allowed]))

(defn input
  [props]
  [:div {:class [base-class
                 (when (:disabled props) disabled-class)
                 (class-names (:class props))]}
   (:prefix props)
   [:input (merge (dissoc props :class :input-class :prefix :suffix)
                  {:class [input-class (class-names (:input-class props))]})]
   (:suffix props)])

(defn zf-input
  [props]
  (let [opts        (:opts props)
        value       (rf/subscribe [::zf/value opts])
        placeholder (when-let [path (:zf/placeholder opts)]
                      (rf/subscribe [::zf/value (assoc opts :zf/path path)]))
        on-change   #(rf/dispatch [::zf/set-value opts (.. % -target -value) {:auto-commit false}])
        on-blur     #(rf/dispatch [::zf/commit-value opts])]
    (fn [props]
      [input
       (merge (dissoc props :opts)
              (when opts
                {:value       (or @value "")
                 :id          (zf/get-id opts)
                 :placeholder (and placeholder @placeholder)
                 :on-change   on-change
                 :on-blur     on-blur}))])))

(defn zf-input-datalist
  [props]
  (let [opts (:opts props)
        datalist (:datalist opts)
        extract-val (fn [enterd-v]
                     (if-let [predefined-val
                              (some (fn [{value :value}]
                                      (when (str/starts-with? enterd-v value) value))
                                    (:data datalist))]
                       predefined-val
                       enterd-v))
        input-props (merge
                      (dissoc props :opts)
                      {:list (:name datalist)}
                      (when opts
                        {:value     (or @(rf/subscribe [::zf/value opts]) "")
                         :id        (zf/get-id opts)
                         :on-change #(rf/dispatch [::zf/set-value opts (extract-val (.. % -target -value)) {:auto-commit false}])
                         :on-blur   #(rf/dispatch [::zf/commit-value opts])}))]
    [:<>
     [:datalist {:id (:name datalist)}
      (for [{:keys [value description]} (:data datalist)
            :let [display (str value " - " description)]]
        [:option {:value display :key display}])]
     [input input-props]]))

(defn zf-number
  [props]
  [input
   (merge
     (dissoc props :opts :float :forbid-negative)
     (when-let [o (:opts props)]
       (let [number-parser (if (:float props) parse-float parse-int)
             forbid-negative (if (:forbid-negative props) #?(:clj (fn [e] (Math/abs e))
                                                             :cljs js/Math.abs) identity)
             comp-fns [forbid-negative number-parser]
             transform-fn (apply comp comp-fns)]
         {:value     (or @(rf/subscribe [::zf/value o]) "")
          :type      "number"
          :on-change #(rf/dispatch [::zf/set-value o (some-> (.. % -target -value) not-empty transform-fn) {:auto-commit false}])
          :on-blur   #(rf/dispatch [::zf/commit-value o])})))])

(defn demo
  []
  [block {:title "Inputs"}
   [input {:placeholder "Default"}]
   [input {:placeholder "Default" :default-value "With value"}]
   [input {:placeholder "Disabled" :disabled true}]
   [input {:placeholder "Disabled" :disabled true :default-value "With value"}]
   [input {:placeholder "With prefix" :class (c [:w 40]) :prefix [:i.far.fa-search {:class (c [:text :gray-500])}]}]
   [input {:placeholder "With suffix" :class (c [:w 40]) :suffix [:i.far.fa-eye {:class (c [:text :gray-500])}]}]])
