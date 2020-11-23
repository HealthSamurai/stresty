(ns anti.checkbox
  (:require
    [anti.util :refer [class-names block]]
    [stylo.core :refer [c c?]]
    [zf.core :as zf]
    [re-frame.core :as rf]))

(def base-class
  (c :appearance-none
     :inline-block
     :align-middle
     :select-none
     [:h 4.5] [:w 4.5]
     [:text :blue-500]
     [:bg :white]
     :border :rounded
     :flex-none
     :cursor-pointer
     [:focus :outline-none :shadow-outline]
     [:hover [:border :blue-500]]
     [:checked
      [:border :transparent]
      [:bg :current]
      :bg-center
      :bg-no-repeat
      {:background-image "url(\"data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='white' xmlns='http://www.w3.org/2000/svg'%3e%3cpath d='M5.707 7.293a1 1 0 0 0-1.414 1.414l2 2a1 1 0 0 0 1.414 0l4-4a1 1 0 0 0-1.414-1.414L7 8.586 5.707 7.293z'/%3e%3c/svg%3e\")"
       :background-size "100% 100%"}]))


(def inverted-class
  (c :appearance-none
     :inline-block
     :align-middle
     :select-none
     [:h 4.5] [:w 4.5]
     [:bg :white]
     :border :rounded
     :flex-none
     :cursor-pointer
     [:focus :outline-none :shadow-outline]
     [:hover [:border :blue-500]]
     [:checked
      :bg-center
      :bg-no-repeat
      {:background-image "url(\"data:image/svg+xml,%3csvg viewBox='0 0 16 16' fill='gray' xmlns='http://www.w3.org/2000/svg'%3e%3cpath d='M5.707 7.293a1 1 0 0 0-1.414 1.414l2 2a1 1 0 0 0 1.414 0l4-4a1 1 0 0 0-1.414-1.414L7 8.586 5.707 7.293z'/%3e%3c/svg%3e\")"
       :background-size "100% 100%"}]))


(defn checkbox
  [props]
  (let [i [:input (merge (dissoc props :class :type)
                         {:class [(if (= "inverted" (:type props)) inverted-class base-class) (class-names (:class props))]
                          :type  "checkbox"})]]
    (if (:label props)
      [:label {:class (c :inline-flex :items-center [:space-x 2])}
       i [:span (:label props)]]
      i)))

(defn checkbox-group
  [props]
  {:pre [(or (nil? (:value props))
             (set? (:value props)))]}
  (into [:div {:class (:class props)}]
        (for [option (:options props)]
          ^{:key (:value option)}
          [checkbox (merge (dissoc props :options :value :class)
                           (dissoc option :class)
                           {:class     [(class-names (:checkbox-class props))
                                        (class-names (:class option))]
                            :checked   (when (:value props)
                                         (contains? (:value props) (:value option)))
                            :on-change #(when-let [on-change
                                                   (some-> (:on-change props)
                                                           (comp (fnil (if (.. % -target -checked) conj disj) #{})))]
                                          (on-change (:value props)
                                                     (:value option)))})])))

(defn zf-checkbox
  [props]
  [checkbox
   (merge
     (dissoc props :opts)
     (when-let [o (:opts props)]
       {:checked   (or @(rf/subscribe [::zf/value o]) false)
        :on-change #(rf/dispatch [::zf/set-value o (.. % -target -checked)])}))])

(defn zf-checkbox-group
  [props]
  [checkbox-group
   (merge
     (dissoc props :opts)
     (when-let [o (:opts props)]
       {:value     (or @(rf/subscribe [::zf/value o]) #{})
        :on-change #(rf/dispatch [::zf/set-value o %])}))])

(defn demo
  []
  [block {:title "Checkboxes"}
   [checkbox {:default-checked true}]
   [checkbox {:label "Personal"}]
   [checkbox-group
    {:class   (c :flex :flex-col [:space-y 1])
     :options [{:label "Option 1"}
               {:label "Option 2"}
               {:label "Option 3"}]}]
   [checkbox-group
    {:class     (c :flex [:space-x 4])
     :value     #{"A"}
     :on-change prn
     :options   [{:label "A" :value "A"}
                 {:label "B" :value "B"}
                 {:label "C" :value "C"}]}]
   [checkbox-group
    {:class   (c :flex :flex-col [:space-y 1])
     :options [{:label "Option 1"}
               {:label "Option 2"}
               {:label "Option 3" :class (c [:text :pink-600])}]}]
   [zf-checkbox
    {:label "Hello"
     :opts  {:zf/root [::form] :zf/path [:zf-checkbox]}}]
   [zf-checkbox-group
    {:class   (c :flex :flex-col [:space-y 1])
     :opts    {:zf/root [::form] :zf/path [:zf-checkbox-group]}
     :options [{:label "Option 1" :value "option-1"}
               {:label "Option 2" :value "option-2"}
               {:label "Option 3" :value "option-3"}]}]])
