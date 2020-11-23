(ns anti.dropdown-datepicker
  (:require
    [stylo.core :refer [c c?]]
    [anti.click-outside]
    [anti.date-input :refer [value-format display-format]]
    [chrono.calendar]
    [chrono.now]
    [chrono.core]
    [zf.core :as zf]
    [zframes.re-frame :as zrf]
    [medley.core :as medley]))


(def dropdown-class
  (c :absolute
     :rounded
     :flex
     :leading-relaxed
     {:box-shadow "0 3px 6px -4px rgba(0,0,0,.12), 0 6px 16px 0 rgba(0,0,0,.08), 0 9px 28px 8px rgba(0,0,0,.05)"}
     :overflow-hidden
     [:bg :white]
     [:z 100]
     [:w "fit-content"]
     [:mt "4px"]
     [:top "100%"]
     [:left "-1px"]
     [:right "-1px"]))


(def preset-class
  (c [:px 2] [:py 1]
     :truncate
     [:w-min 40]
     :cursor-default
     [:hover [:bg :gray-200]]))


(def selected-preset-class
  (c [:bg :blue-100]))


(def calendar-class
  (c [:p 1]
     :items-center
     :cursor-default
     :grid
     {:grid-template-columns "repeat(7, 1fr)"
      :justify-items         "center"}))


(def button-class
  (c [:w 7] [:h 7]
     :flex
     :text-xs
     :justify-center
     :items-center
     :rounded
     :leading-none
     :cursor-pointer
     [:text :gray-500]
     [:hover [:bg :gray-200]]))


(def day-class
  (c [:w 7] [:h 7]
     :flex
     :justify-center
     :items-center
     :leading-none
     :text-xs
     [:text :gray-500]))


(def option-class
  (c [:w 7] [:h 7]
     :flex
     :text-xs
     :justify-center
     :items-center
     :rounded
     :leading-none))


(def active-option-class
  (c [:bg :gray-200]))


(def selected-option-class
  (c [:bg :blue-100]))


(def disabled-option-class
  (c [:text :gray-500]))


(zrf/reg-sub
  opened
  (fn [[_ opts]]
    (zrf/subscribe [::zf/state opts]))
  (fn [state _]
    (get state :opened)))


(zrf/reg-sub
  active
  (fn [[_ opts]]
    (zrf/subscribe [::zf/state opts]))
  (fn [state _]
    (get state :active)))


(zrf/reg-sub
  calendar
  (fn [[_ opts]]
    (zrf/subscribe [active opts]))
  (fn [active _]
    (when active
      (:cal (chrono.calendar/for-month (:year active) (:month active))))))


(zrf/reg-event-fx
  activate
  (fn [{:keys [db]} [_ opts value]]
    {:db (zf/set-state db opts [:active] value)}))


(zrf/reg-event-fx
  reset-active
  (fn [{:keys [db]} [_ opts]]
    {:db (zf/set-state db opts [:active]
                       (or (some-> (zf/value db opts) :value (doto prn) (chrono.core/parse value-format))
                           (dissoc (chrono.now/today) :tz)))}))


(zrf/reg-event-fx
  open
  (fn [{:keys [db]} [_ opts]]
    {:dom/focus  (zf/get-id opts)
     :dispatch-n [[reset-active opts]]
     :db         (zf/set-state db opts [:opened] true)}))


(zrf/reg-event-fx
  change-active
  (fn [{:keys [db]} [_ opts diff]]
    (let [{:keys [opened active]} (zf/state db opts)]
      {:dispatch-n [(when (and opened active)
                      [activate opts (chrono.core/+ active diff)])
                    (when (not opened)
                      [open opts])]})))


(zrf/reg-event-fx
  close
  (fn [{:keys [db]} [_ opts]]
    {:db (zf/set-state db opts [:opened] false)}))


(zrf/reg-event-fx
  select
  (fn [_ [_ opts value]]
    {:dom/focus  (zf/get-id opts :input)
     :dispatch-n [[close opts]
                  [::zf/set-value opts
                   {:value (some-> value
                                   (chrono.core/format value-format))}]]}))


(zrf/reg-event-fx
  select-preset
  (fn [_ [_ opts value]]
    {:dom/focus  (zf/get-id opts :input)
     :dispatch-n [[close opts]
                  [::zf/set-value opts value]]}))


(zrf/reg-event-fx
  search
  (fn [{:keys [db]} [_ opts value]]
    (let [{:keys [opened active]} (zf/state db opts)
          parsed (->> (chrono.core/parse value display-format)
                      (medley/remove-vals zero?))]
      {:dispatch-n [[activate opts (merge active parsed)]
                    (when-not opened [open opts])]})))


(zrf/reg-event-fx
  select-active
  (fn [{:keys [db]} [_ opts]]
    (let [{:keys [active]} (zf/state db opts)]
      {:dispatch-n [(when active [select opts active])]})))


(zrf/reg-event-fx
  toggle-opened
  (fn [{:keys [db]} [_ opts]]
    (let [{:keys [opened]} (zf/state db opts)]
      {:dispatch-n [(if opened [close opts] [open opts])]})))


(zrf/reg-sub
  presets
  (fn [[_ opts]]
    [(zrf/subscribe [::zf/schema opts])
     (zrf/subscribe [::zf/state opts])])

  (fn [[schema state] _]
    (:options schema)))


(defn dropdown-datepicker
  [{:keys [opts]}]
  (let [presets              (zrf/subscribe [presets opts])
        opened               (zrf/subscribe [opened opts])
        value                (zrf/subscribe [::zf/value opts])
        active               (zrf/subscribe [active opts])
        calendar             (zrf/subscribe [calendar opts])
        on-wrapper-click     (fn [_] (zrf/dispatch [toggle-opened opts]))
        on-click-outside     (fn [_] (zrf/dispatch [close opts]))
        on-preset-click      (fn [date _] (zrf/dispatch [select-preset opts date]))
        on-option-click      (fn [date _] (zrf/dispatch [select opts date]))
        on-option-mouse-move (fn [date _] (zrf/dispatch [activate opts date]))
        on-next-year         (fn [_] (zrf/dispatch [change-active opts {:year +1}]))
        on-prev-year         (fn [_] (zrf/dispatch [change-active opts {:year -1}]))
        on-next-month        (fn [_] (zrf/dispatch [change-active opts {:month +1}]))
        on-prev-month        (fn [_] (zrf/dispatch [change-active opts {:month -1}]))]
    (fn [_ & children]
      (let [presets  @presets
            opened   @opened
            value    @value
            active   @active
            calendar @calendar]
        [:div {:class (c :relative :inline-block)}
         (into [:div {:class    (c :inline-block :cursor-pointer)
                      :on-click on-wrapper-click}] children)
         (when opened
           [anti.click-outside/click-outside {:on-click on-click-outside}
            [:div {:class (c :absolute [:w 200])}
             [:div {:class dropdown-class}
              [:div {:class (c [:py 1])}
               (for [preset presets]
                 [:div {:key      (:value preset)
                        :id       (:id preset)
                        :on-click (partial on-preset-click preset)
                        :class    [preset-class
                                   (when (:anti/selected preset) selected-preset-class)]}
                  (or (:display preset) (pr-str preset))])]

              [:div {:class calendar-class}
               [:div {:class button-class :on-click on-prev-year} [:i.far.fa-angle-double-left]]
               [:div {:class button-class :on-click on-prev-month} [:i.far.fa-angle-left]]
               [:div {:class (c [:space-x 1] {:grid-column "auto / span 3"})}
                [:span (get-in chrono.calendar/month-names [(:month active) :short])]
                [:span (:year active)]]
               [:div {:class button-class :on-click on-next-month} [:i.far.fa-angle-right]]
               [:div {:class button-class :on-click on-next-year} [:i.far.fa-angle-double-right]]

               (for [[_ {day :name}] chrono.calendar/weeks]
                 [:div {:key   day
                        :class day-class}
                  (subs day 0 2)])

               (for [[index week] (map-indexed vector calendar)]
                 [:<> {:key index}
                  (for [[index date] (map-indexed vector week)
                        :let [alien?    (not (:current date))
                              date      (dissoc date :current)
                              active?   (= active date)
                              selected? (= value date)]]
                    [:div {:key           index
                           :class         [option-class
                                           (cond selected? selected-option-class
                                                 active? active-option-class
                                                 alien? disabled-option-class)]
                           :on-click      (partial on-option-click date)
                           :on-mouse-move (when-not (or active? alien?)
                                            (partial on-option-mouse-move date))}
                     (:day date)])])]]]])]))))
