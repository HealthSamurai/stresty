(ns anti.dropdown-menu
  (:require
    [stylo.core :refer [c]]
    [anti.util :refer [block class-names]]
    [anti.button]
    [anti.click-outside]
    [re-frame.core :as rf]
    [zframes.re-frame :as zrf]
    [anti.dropdown-select :as dropdown-select]
    [zf.core :as zf]))


(defn zf-dropdown-menu
  [props]
  (let [state       @(rf/subscribe [::zf/state (:opts props)])
        value       @(rf/subscribe [::zf/value (:opts props)])
        options     @(rf/subscribe [dropdown-select/options (:opts props)])
        open        (fn [e] (rf/dispatch [dropdown-select/open (:opts props)]) (doto e .preventDefault .stopPropagation))
        close       (fn [_] (rf/dispatch [dropdown-select/close (:opts props)]))
        on-pick     (fn [v] (rf/dispatch [dropdown-select/pick (:opts props) (:value v)]))
        on-key-down (fn [e] (cond (or (= "ArrowUp" (.-key e)) (and (.-ctrlKey e) (= "k" (.-key e))))
                                  (do (zrf/dispatch [dropdown-select/move-selection (:opts props) -1]) (.preventDefault e))
                                  (or (= "ArrowDown" (.-key e)) (and (.-ctrlKey e) (= "j" (.-key e))))
                                  (do (zrf/dispatch [dropdown-select/move-selection (:opts props) +1]) (.preventDefault e))
                                  (= "Enter" (.-key e))
                                  (do (zrf/dispatch [dropdown-select/pick-selection (:opts props)]) (.preventDefault e))
                                  (= "Escape" (.-key e))
                                  (do (zrf/dispatch [dropdown-select/close (:opts props)]) (.preventDefault e))))]
    [:div {:class (c :relative)}
     [:button {:on-key-down      on-key-down
               :type             "button"
               :title            (:title props)
               :on-click-capture (if (:open state) close open)
               :class            [anti.button/base-class]}
      [:i {:class [(class-names (:icon props)) (c [:mr 2])]}]
      (or value (:title props) "None")]

     (when (:open state)
       [anti.click-outside/click-outside {:on-click close}
        [:div {:class (c :absolute [:bg :white] [:mt 1] :border :rounded
                         {:left       0 :top "100%" :z-index 4 :min-width "150px" :max-width "300px"
                          :box-shadow "0 3px 6px -4px rgba(0,0,0,.12), 0 6px 16px 0 rgba(0,0,0,.08), 0 9px 28px 8px rgba(0,0,0,.05)"})}
         (->> options
              (map-indexed
               (fn [i option]
                 (let [active (c [:px 3] [:py 1.5] [:bg :blue-100] [:hover :cursor-pointer])
                       hover  (c [:px 3] [:py 1.5] [:hover [:bg :gray-200] :cursor-pointer])]
                   [:div {:key      i
                          :on-click #(on-pick option)
                          :class    (if (= (:value option) value) active hover)}
                    (:display option)]))))]])]))


(defn demo
  []
  [block {:title "Altrenative Dropdown"}
   [zf-dropdown-menu {:title "Status"
                      :icon      "far fa-flag"
                      :opts      {:zf/root [:anti/index :form]
                                  :zf/path [:alt-dropdown]}}]])
