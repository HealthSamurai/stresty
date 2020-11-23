(ns anti.date-input
  (:require
    [anti.util :refer [class-names block ratom cursor current-timestamp]]
    [anti.mask-input :refer [set-selection get-selection]]
    [input-mask.core :as mask]
    [chrono.calendar]
    [chrono.now]
    [chrono.core]
    [stylo.core :refer [c]]
    [zf.core :as zf]
    [zframes.dom]
    [zframes.re-frame :as zrf]
    [clojure.string :as str]
    [medley.core :as medley]))


(def display-format
  [:month "/" :day "/" :year])


(def value-format
  [:year "-" :month "-" :day])


(defn format->pattern
  [format]
  (->> format
       (map (some-fn
              {:year "1111" :month "11" :day "11" :hour "11" :min "11" :sec "11" :ms "111" :tz "11"}
              identity))
       (str/join "")))


(defn get-mask*
  [db opts]
  (or (zf/state db opts [:search-mask])
      (mask/make-input-mask
        {:pattern (format->pattern display-format)})))


(def base-class
  (c :relative
     :inline-flex
     :w-max-full
     :w-full
     [:leading-relaxed]
     [:pr 7]
     :border
     :rounded
     [:bg :white]
     :items-center
     :transition-all [:duration 200] :ease-in-out
     [:focus-within :shadow-outline [:border :blue-500]]
     [:hover [:border :blue-500]]
     [:pseudo "[disabled]" [:bg :gray-200] [:border :gray-400] [:text :gray-500] :cursor-not-allowed]))


(def icon-class
  (c [:text :gray-400]
     [:w 7]
     :absolute
     [:right 0]
     [:top 0]
     [:bottom 0]
     :justify-center
     :flex
     :items-center
     [:before [:mt 0.5]]
     [:pseudo ":[disabled]" :cursor-not-allowed]
     [:pseudo ":not([disabled])" [:hover [:text :gray-500]]]))


(def value-class
  (c :absolute
     :truncate
     [:left 2]
     [:right 6]
     :pointer-events-none))


(def search-class
  (c [:text :gray-700]
     [:bg :transparent]
     [:px 2] [:py 1]
     :flex-1
     :cursor-default
     [:disabled :cursor-not-allowed]
     [:focus :outline-none :cursor-text]))


(def dropdown-class
  (c :absolute
     :rounded
     :leading-relaxed
     {:box-shadow "0 3px 6px -4px rgba(0,0,0,.12), 0 6px 16px 0 rgba(0,0,0,.08), 0 9px 28px 8px rgba(0,0,0,.05)"}
     :overflow-x-hidden
     :overflow-y-auto
     [:h-max 60]
     [:bg :white]
     [:z 100]
     [:mt "4px"]
     [:top "100%"]
     [:left "-1px"]
     [:p 1]
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
                       (or (chrono.core/parse (zf/value db opts) value-format)
                           (dissoc (chrono.now/today) :tz)))}))


(zrf/reg-event-fx
  open
  (fn [{:keys [db]} [_ opts]]
    {:dom/focus  (zf/get-id opts)
     :dispatch-n [[reset-active opts]]
     :db         (-> db
                     (zf/set-state opts [:opened] true)
                     (zf/set-state opts [:search-mask]
                                   (-> (get-mask* db opts)
                                       (assoc :selection {:start 0 :end 0})
                                       (mask/set-value
                                         (some-> (not-empty (zf/value db opts))
                                                 (chrono.core/parse value-format)
                                                 (chrono.core/format display-format))))))}))


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
    {:db (-> db
             (zf/set-state opts [:opened] false)
             (zf/set-state opts [:search-mask]
                           (-> (get-mask* db opts)
                               (assoc :value nil))))}))


(zrf/reg-event-fx
  select
  (fn [_ [_ opts value]]
    {:dom/focus  (zf/get-id opts :input)
     :dispatch-n [[close opts]
                  [::zf/set-value opts
                   (some-> value
                           (chrono.core/format value-format))]]}))


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


(zrf/reg-event-fx
  search-input
  (fn [{db :db} [_ opts selection char]]
    (let [mask (get-mask* db opts)
          mask (assoc mask :selection selection)
          mask (or (mask/input mask char) mask)]
      {:db       (zf/set-state db opts [:search-mask] mask)
       :dispatch [search opts (mask/get-value mask)]})))


(zrf/reg-event-fx
  search-backspace
  (fn [{db :db} [_ opts selection]]
    (let [mask (get-mask* db opts)
          mask (assoc mask :selection selection)
          mask (or (mask/backspace mask) mask)]
      {:db       (zf/set-state db opts [:search-mask] mask)
       :dispatch [search opts (mask/get-value mask)]})))


(zrf/reg-sub
  search-mask
  (fn [[_ opts]]
    (zrf/subscribe [::zf/state opts]))
  (fn [state _]
    (:search-mask state)))


(zrf/reg-sub
  search-selection
  (fn [[_ opts]]
    (zrf/subscribe [search-mask opts]))
  (fn [mask _]
    (:selection mask)))


(zrf/reg-sub
  search-value
  (fn [[_ opts]]
    (zrf/subscribe [search-mask opts]))
  (fn [search-mask _]
    (mask/get-value search-mask)))


(zrf/reg-sub
  search-length
  (fn [[_ opts]]
    (zrf/subscribe [search-mask opts]))
  (fn [search-mask _]
    (get-in search-mask [:pattern :length])))


(defn zf-date-input
  [{:keys [opts]}]
  (let [selection-c             (volatile! nil)
        container-ref           (volatile! nil)
        search-value            (zrf/subscribe [search-value opts])
        opened                  (zrf/subscribe [opened opts])
        value                   (zrf/subscribe [::zf/value opts])
        active                  (zrf/subscribe [active opts])
        calendar                (zrf/subscribe [calendar opts])
        search-selection        (zrf/subscribe [search-selection opts])
        search-length           (zrf/subscribe [search-length opts])
        on-container-mouse-down (fn [event]
                                  (.preventDefault event)
                                  (zrf/dispatch [toggle-opened opts]))
        on-container-key-down   (fn [event]
                                  (case (.-key event)
                                    "Escape" (do (.preventDefault event) (zrf/dispatch [close opts]))
                                    "Enter" (do (.preventDefault event) (zrf/dispatch [select-active opts]))
                                    "ArrowUp" (do (.preventDefault event) (zrf/dispatch [change-active opts {:day -1}]))
                                    "ArrowDown" (do (.preventDefault event) (zrf/dispatch [change-active opts {:day +1}]))
                                    nil))
        on-search-key-press     (fn [event]
                                  (when-not (or (.-metaKey event) (.-altKey event) (.-ctrlKey event) (.-metaKey event) (= "Enter" (.-key event)))
                                    (.preventDefault event)
                                    (zrf/dispatch-sync [search-input opts (or @selection-c (get-selection (.-target event))) (.-key event)])
                                    (vreset! selection-c (assoc @search-selection :ts (current-timestamp)))
                                    (set-selection (.-target event) selection-c)))
        on-search-key-down      (fn [event]
                                  (when (= "Backspace" (.-key event))
                                    (.preventDefault event)
                                    (zrf/dispatch-sync [search-backspace opts (or @selection-c (get-selection (.-target event)))])
                                    (vreset! selection-c (assoc @search-selection :ts (current-timestamp)))
                                    (set-selection (.-target event) selection-c)))
        on-clear-mouse-down     (fn [event]
                                  (.stopPropagation event)
                                  (.preventDefault event)
                                  (zrf/dispatch [select opts nil]))
        on-container-ref        (fn [ref]
                                  (vreset! container-ref ref))
        on-option-mouse-down    (fn [date event]
                                  (.preventDefault event)
                                  (zrf/dispatch [select opts date]))
        on-option-mouse-move    (fn [date _]
                                  (zrf/dispatch [activate opts date]))
        on-next-year            (fn [event]
                                  (.stopPropagation event)
                                  (.preventDefault event)
                                  (zrf/dispatch [change-active opts {:year +1}]))
        on-prev-year            (fn [event]
                                  (.stopPropagation event)
                                  (.preventDefault event)
                                  (zrf/dispatch [change-active opts {:year -1}]))
        on-next-month           (fn [event]
                                  (.stopPropagation event)
                                  (.preventDefault event)
                                  (zrf/dispatch [change-active opts {:month +1}]))
        on-prev-month           (fn [event]
                                  (.stopPropagation event)
                                  (.preventDefault event)
                                  (zrf/dispatch [change-active opts {:month -1}]))]

    (fn [props]
      (let [search-value  @search-value
            opened        @opened
            value         (some-> @value (chrono.core/parse value-format))
            active        @active
            calendar      @calendar
            search-length @search-length
            clearable     (:clearable props true)
            dropdown      (:dropdown props true)
            disabled      (boolean (:disabled props))
            searchable    (boolean (:searchable props true))]
        [:div {:class         [base-class (class-names (:class props))]
               :disabled      disabled
               :ref           on-container-ref
               :on-mouse-down (when-not disabled on-container-mouse-down)
               :on-key-down   (when-not disabled on-container-key-down)}

         (when (or (not searchable) (not opened))
           [:div {:class [value-class
                          (when (seq search-value) (c [:invisible]))
                          (when (empty? value) (c [:text :gray-500]))]}
            (if value
              (chrono.core/format value display-format)
              (:placeholder props))])

         [:input {:class           [search-class (class-names (:search-class props))
                                    (when-not opened (c [:opacity 0]))]
                  :id              (zf/get-id opts)
                  :disabled        disabled
                  :value           search-value
                  :read-only       (not searchable)
                  ;;:on-change       #(zrf/dispatch [search opts (.. % -target -value)])
                  :on-blur         #(zrf/dispatch [close opts])
                  :size            search-length
                  :on-change       identity
                  :on-key-press    on-search-key-press
                  :on-key-down     on-search-key-down
                  :placeholder     (if (and opened searchable value)
                                     (chrono.core/format value display-format)
                                     (:placeholder props))
                  :auto-capitalize "none"
                  :auto-complete   "off"
                  :auto-correct    "off"
                  :spell-check     "false"}]

         [:div {:class    icon-class
                :disabled disabled}
          (cond
            (and clearable (some? value) (not disabled))
            [:i.fal.fa-times {:class         (c :cursor-pointer)
                              :on-mouse-down on-clear-mouse-down}]

            :else
            [:i.fal.fa-calendar {:class (c :text-xs)}])]

         (when (and opened dropdown)
           [:div {:class [dropdown-class (class-names (:dropdown-class props))]}
            [:div {:class button-class :on-mouse-down on-prev-year} [:i.far.fa-angle-double-left]]
            [:div {:class button-class :on-mouse-down on-prev-month} [:i.far.fa-angle-left]]
            [:div {:class (c [:space-x 1] {:grid-column "auto / span 3"})}
             [:span (get-in chrono.calendar/month-names [(:month active) :short])]
             [:span (:year active)]]
            [:div {:class button-class :on-mouse-down on-next-month} [:i.far.fa-angle-right]]
            [:div {:class button-class :on-mouse-down on-next-year} [:i.far.fa-angle-double-right]]

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
                        :on-mouse-down (partial on-option-mouse-down date)
                        :on-mouse-move (when-not (or active? alien?)
                                         (partial on-option-mouse-move date))}
                  (:day date)])])])]))))


(zrf/reg-event-db
  init-demo
  (fn [db _]
    (-> db
        (assoc-in (zf/value-path {:zf/root [::db] :zf/path [:date :two]})
                  "2020-07-22"))))


(defn demo
  []
  (zrf/dispatch [init-demo])
  (fn []
    [block {:title "Date input"}
     [zf-date-input {:placeholder "Default"
                     :dropdown    false
                     :opts        {:zf/root [::db] :zf/path [:date :one]}}]
     [zf-date-input {:placeholder "With initial value"
                     :opts        {:zf/root [::db] :zf/path [:date :two]}}]]))

