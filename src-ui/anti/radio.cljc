(ns anti.radio
  (:require
    [anti.util :refer [class-names block]]
    [stylo.core :refer [c c?]]
    [zf.core :as zf]
    [zframes.re-frame :as zrf]))

(def base-class
  (c :appearance-none
     :inline-block
     :align-middle
     :select-none
     [:h 4.5] [:w 4.5]
     [:text :blue-500]
     [:bg :white]
     :border [:rounded :full]
     :flex-none
     :cursor-pointer
     {:flex-shrink 0}
     [:focus :outline-none :shadow-outline]
     [:hover [:border :blue-500]]
     [:checked
      [:border :transparent]
      [:bg :current]
      :bg-center
      :bg-no-repeat
      {:background-image "url(\"data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='white' xmlns='http://www.w3.org/2000/svg'%3e%3ccircle cx='8' cy='8' r='3'/%3e%3c/svg%3e\")"
       :background-size "100% 100%"}]))

(defn radio
  [props]
  (let [i [:input (merge (dissoc props :class :label-class)
                         {:class [base-class (class-names (:class props))]
                          :type  "radio"})]]
    (if (:label props)
      [:label {:class [(c :inline-flex :items-center [:space-x 2])
                       (class-names (:label-class props))]}
       i [:span (:label props)]]
      i)))


(defn radio-group
  [props]
  (into [:div {:class (:class props)}]
        (for [option (:options props)]
          ^{:key (:value option)}
          [radio (merge (dissoc props :options :value :class :radio-class)
                        (dissoc option :class)
                        {:class     [(class-names (:radio-class props)) (class-names (:class option))]
                         :checked   (when (:value props) (= (:value option) (:value props)))
                         :on-change #(when-let [on-change (:on-change props)]
                                       (on-change (:value option)))})])))

(defn zf-radio
  [props]
  [radio
   (merge
     (dissoc props :opts)
     {:checked   (or @(zrf/subscribe [::zf/value (:opts props)]) false)
      :on-change #(zrf/dispatch [::zf/set-value (:opts props) (.. % -target -checked)])})])

(defn zf-radio-group
  [props]
  [radio-group
   (merge
     (dissoc props :opts)
     {:value     (or @(zrf/subscribe [::zf/value (:opts props)]) [])
      :on-change #(zrf/dispatch [::zf/set-value (:opts props) %])
      :options   (:options @(zrf/subscribe [::zf/schema (:opts props)]))})])


(zrf/reg-event-db
  init-demo
  (fn [db _]
    (-> db
        (assoc-in (zf/schema-path {:zf/root [::form] :zf/path [:zf-radio-group]})
                  {:options [{:label "Option 1" :value "option-1"}
                             {:label "Option 2" :value "option-2"}
                             {:label "Option 3" :value "option-3"}]}))))


(defn demo
  []
  (zrf/dispatch [init-demo])
  (fn []
    [block {:title "Radios"}
     [radio {:default-checked true}]
     [radio {:label "Personal"}]
     [radio-group
      {:class   (c :flex :flex-col [:space-y 1])
       :name    "radio-1"
       :options [{:label "Option 1" :value "1"}
                 {:label "Option 2" :value "2"}
                 {:label "Option 3" :value "3"}]}]
     [radio-group
      {:class   (c :flex [:space-x 4])
       :name    "radio-2"
       :options [{:label "A" :value "1"}
                 {:label "B" :value "2"}
                 {:label "C" :value "3"}]}]
     [radio-group
      {:class       (c :flex :flex-col [:space-y 1])
       :name        "radio-3"
       :radio-class (c [:text :green-600])
       :options     [{:label "Option 1" :value "1"}
                     {:label "Option 2" :value "2"}
                     {:label "Option 3" :value "3" :class (c [:text :teal-600])}]}]
     [radio-group
      {:class   (c :flex :flex-col [:space-y 1])
       :name    "radio-4"
       :options [{:label "Option 1" :value "1"}
                 {:label "Option 2" :value "2"}
                 {:label "Option 3" :value "3" :class (c [:text :pink-600])}]}]
     [zf-radio
      {:label "Hello"
       :opts  {:zf/root [::form] :zf/path [:zf-radio]}}]
     [zf-radio-group
      {:class (c :flex :flex-col [:space-y 1])
       :opts  {:zf/root [::form] :zf/path [:zf-radio-group]}}]]))
