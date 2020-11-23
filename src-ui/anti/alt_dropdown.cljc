(ns anti.alt-dropdown
  (:require
    [stylo.core :refer [c]]
    [anti.util :refer [block class-names]]
    [anti.click-outside :refer [click-outside]]
    [re-frame.core :as rf]
    [zframes.re-frame :as zrf]
    [clojure.string :as str]
    [anti.dropdown-select-model]
    [zf.core :as zf]))

(defn alt-dropdown
  [props]
  (let [state       @(rf/subscribe [:zf.core/state props])
        value       @(rf/subscribe [:zf.core/value props])
        options     @(rf/subscribe [:anti/dropdown-select-options props])
        open        (fn [e] (rf/dispatch [:anti/dropdown-select-open props]) (doto e .preventDefault .stopPropagation))
        close       (fn [_] (rf/dispatch [:anti/dropdown-select-close props]))
        on-pick     (fn [v] (rf/dispatch [:anti/dropdown-select-pick props (:value v)]))
        on-key-down (fn [e]
                      #?(:cljs (let [pressed-key (.-key e)
                                     ctrl        (.-ctrlKey e)]
                                 (when (or ctrl (#{"ArrowUp" "ArrowDown" "Enter" "Escape"} pressed-key))
                                   (doto e .preventDefault .stopPropagation))
                                 (cond (or (= "ArrowUp" pressed-key) (and ctrl (= "k" pressed-key)))
                                       (rf/dispatch [:anti/dropdown-select-move-selection props -1])
                                       (or (= "ArrowDown" pressed-key) (and ctrl (= "j" pressed-key)))
                                       (rf/dispatch [:anti/dropdown-select-move-selection props +1])

                                       (= "Enter" pressed-key)
                                       (rf/dispatch [:anti/dropdown-select-pick-selection props])
                                       (= "Escape" pressed-key)
                                       (rf/dispatch [:anti/dropdown-datepicker-close props])))))]
    [click-outside {:on-close close}
     [:div {:class (c :relative)}
      [:div {:on-key-down      on-key-down
             :title            (:title props)
             :on-click-capture (if (:open state) close open)
             :class            (c [:px 3] [:py 1] :border :rounded :flex :items-baseline [:bg :gray-100] [:hover [:bg :gray-200] :cursor-pointer]
                                  {:box-shadow          "0 1px 0 rgba(27,31,35,.04), inset 0 1px 0 hsla(0,0%,100%,.25)"
                                   :transition-duration "0.2s"})}
       [:i {:class [(class-names (:icon props)) (c [:mr 2])]}]
       (or value (:title props) "None")]

      (when (:open state)
        [:div {:class (c :absolute [:bg :white] [:mt 1] :border :rounded
                         {:left       0 :top "100%" :z-index 2 :min-width "150px" :max-width "300px"
                          :box-shadow "0 3px 6px -4px rgba(0,0,0,.12), 0 6px 16px 0 rgba(0,0,0,.08), 0 9px 28px 8px rgba(0,0,0,.05)"})}
         (->> options
              (map-indexed
               (fn [i option]
                 (let [active (c [:px 3] [:py 1.5] [:bg :blue-100] [:hover :cursor-pointer])
                       hover  (c [:px 3] [:py 1.5] [:hover [:bg :gray-200] :cursor-pointer])]
                   [:div {:key      i
                          :on-click #(on-pick option)
                          :class    (if (= (:value option) value) active hover)}
                    (:display option)]))))])]]))


(defn demo
  []
  [block {:title "Altrenative Dropdown"}
   [alt-dropdown {:zf/root [:anti/index :form]
                  :zf/path [:alt-dropdown]
                  :title   "Status"
                  :icon    "far fa-flag"}]])
