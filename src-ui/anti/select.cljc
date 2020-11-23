(ns anti.select
  (:require
    [anti.util :refer [class-names block ratom cursor]]
    [stylo.core :refer [c c?]]
    [zf.core :as zf]
    [zframes.dom]
    [zframes.re-frame :as zrf]
    #?(:cljs [reagent.core :as r])
    [clojure.string :as str]))


(def default-id-fn identity)


(defmulti search-fn
  (fn [k options term]
    k))


(defmethod search-fn :default
  [_ options term]
  (->> options
       (keep
         (fn [option]
           (when-let [index (str/index-of
                              (some-> (str option) (str/lower-case))
                              (some-> term (str/lower-case)))]
             [option index])))
       (sort-by second)
       (mapv first)))


(defmulti value-fn
  (fn [k option]
    k))


(defmethod value-fn
  :default [k option]
  (if (ifn? k)
    (k option)
    option))


(defn scroll-into-view
  [node]
  #?(:cljs (-> node (.scrollIntoView #js {:block "nearest"}))))


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
     :w-full
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
     [:py 1]
     [:z 100]
     [:mt "4px"]
     [:top "100%"]
     [:left "-1px"]
     [:right "-1px"]))


(def option-class
  (c [:px 2] [:py 1]
     :truncate
     :cursor-default))


(def active-option-class
  (c [:bg :gray-200]))


(def selected-option-class
  (c))


(def empty-class
  (c [:px 2] [:py 1]
     :truncate
     :cursor-default
     :text-center
     [:text :gray-500]))


(defn find-option
  [id-fn options value]
  (->> options
       (filter #(= (id-fn %) (id-fn value)))
       (first)))


(defn find-sibling-option
  [id-fn options value distance]
  (or (some (fn [[index option]]
              (if (= (id-fn option) (id-fn value))
                (nth options (mod (+ index distance) (count options)))))
            (map-indexed vector options))
      (first options)))


(zrf/reg-sub
  search-value
  (fn [[_ opts]]
    (zrf/subscribe [::zf/state opts]))
  (fn [state _]
    (or (get state :search-value) "")))


(zrf/reg-sub
  loading
  (fn [[_ opts]]
    (zrf/subscribe [::zf/state opts]))
  (fn [state _]
    (get state :loading)))


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
  should-scroll
  (fn [[_ opts]]
    (zrf/subscribe [::zf/state opts]))
  (fn [state _]
    (get state :should-scroll)))


(zrf/reg-sub
  options
  (fn [[_ opts]]
    (zrf/subscribe [::zf/schema opts]))
  (fn [schema _]
    (get schema :options)))


(zrf/reg-sub
  loaded-options
  (fn [[_ opts]]
    [(zrf/subscribe [::zf/schema opts])
     (zrf/subscribe [::zf/state opts])])
  (fn [[schema state] _]
    (when (:http schema)
      (vec (get-in state [:options])))))


(zrf/reg-sub
  current-options
  (fn [[_ opts]]
    [(zrf/subscribe [::zf/schema opts])
     (zrf/subscribe [loaded-options opts])
     (zrf/subscribe [options opts])
     (zrf/subscribe [search-value opts])])
  (fn [[schema loaded-options options search-value] _]
    (let [{:keys [http search-fn-key]} schema]
      (if http
        loaded-options
        (search-fn search-fn-key options search-value)))))


(def inject-current-options
  (zrf/inject-cofx
    :sub
    (fn [[_ opts] _]
      ^:ignore-dispose
      [current-options opts])))


(zrf/reg-event-fx
  set-should-scroll
  (fn [{:keys [db]} [_ opts bool]]
    {:db (zf/set-state db opts [:should-scroll] bool)}))


(zrf/reg-event-fx
  activate
  (fn [{:keys [db]} [_ opts value]]
    {:db         (zf/set-state db opts [:active] value)
     :dispatch-n [[set-should-scroll opts true]]}))


(zrf/reg-event-fx
  reset-active
  [inject-current-options]
  (fn [{:keys [db] ::keys [current-options]} [_ opts]]
    (let [{:keys [id-fn] :or {id-fn default-id-fn}} (zf/schema db opts)]
      {:dispatch-n [[activate opts (first current-options)]]})))


(zrf/reg-event-fx
  load-options-ok
  (fn [{:keys [db]} [_ {:keys [opts data]}]]
    {:db (-> db
             (zf/set-state opts [:loading] false)
             (zf/set-state opts [:options] data))
     :dispatch-n [[activate opts (first data)]]}))


(zrf/reg-event-fx
  load-options-error
  (fn [{:keys [db]} [_ {:keys [opts]}]]
    {:db (zf/set-state db opts [:loading] false)}))


(zrf/reg-event-fx
 load-options
 (fn [{:keys [db]} [_ opts search-value]]
   (when-let [http (:http (zf/schema db opts))]
     {:db         (zf/set-state db opts [:loading] true)
      :http/fetch (let [aurl (get-in http [:params :alternative_url])]
                    (if (and (nil? search-value)
                             (not (nil? aurl)))
                      {:uri aurl
                       :unbundle true
                       :success  {:event load-options-ok :opts opts}
                       :error    {:event load-options-error :opts opts}}
                      (merge (update http :params merge {(:q http :q) search-value})
                             {:unbundle true
                              :success  {:event load-options-ok :opts opts}
                              :error    {:event load-options-error :opts opts}})))})))


(zrf/reg-event-fx
  load-options-debounce
  (fn [_ [_ opts search-value]]
    {:dispatch-debounce
     {:delay 300
      :key   [::load-options opts]
      :event [load-options opts search-value]}}))


(zrf/reg-event-fx
  open
  (fn [{:keys [db]} [_ opts]]
    {:db         (zf/set-state db opts [:opened] true)
     :dom/focus  (zf/get-id opts)
     :dispatch-n [[reset-active opts]
                  [load-options opts nil]
                  [set-should-scroll opts true]]}))


(zrf/reg-event-fx
  close
  (fn [{:keys [db]} [_ opts]]
    {:db (-> db
             (zf/set-state opts [:opened] false)
             (zf/set-state opts [:search-value] nil))}))


(zrf/reg-event-fx
  select
  (fn [{:keys [db]} [_ opts value]]
    (let [schema (zf/schema db opts)]
      {:dom/focus  (zf/get-id opts :input)
       :dispatch-n [[close opts]
                    [::zf/set-value opts (some->> value (value-fn (:value-fn schema)))]]})))


(zrf/reg-event-fx
  search
  (fn [{:keys [db]} [_ opts value]]
    (let [{:keys [opened]} (zf/state db opts)]
      {:db         (zf/set-state db opts [:search-value] value)
       :dispatch-n [[reset-active opts]
                    [load-options-debounce opts value]
                    (when-not opened [open opts])]})))


(zrf/reg-event-fx
  select-active
  (fn [{:keys [db]} [_ opts]]
    (let [{:keys [active]} (zf/state db opts)]
      {:dispatch-n [(when active [select opts active])]})))


(zrf/reg-event-fx
  activate-sibling
  [inject-current-options]
  (fn [{:keys [db] ::keys [current-options]} [_ opts distance]]
    (let [{:keys [opened active]} (zf/state db opts)
          {:keys [id-fn] :or {id-fn default-id-fn}} (zf/schema db opts)]
      {:dispatch-n [(when (and opened active)
                      [activate opts (find-sibling-option id-fn current-options active distance)])
                    (when (not opened)
                      [open opts])]})))


(zrf/reg-event-fx
  toggle-opened
  (fn [{:keys [db]} [_ opts]]
    (let [{:keys [opened]} (zf/state db opts)]
      {:dispatch-n [(if opened [close opts] [open opts])]})))


(defn zf-select
  [{:keys [opts clear-value]}]
  (let [should-scroll           (zrf/subscribe [should-scroll opts])
        search-value            (zrf/subscribe [search-value opts])
        opened                  (zrf/subscribe [opened opts])
        value                   (zrf/subscribe [::zf/value opts])
        schema                  (zrf/subscribe [::zf/schema opts])
        active                  (zrf/subscribe [active opts])
        current-options         (zrf/subscribe [current-options opts])
        loading                 (zrf/subscribe [loading opts])
        on-container-mouse-down (fn [event]
                                  (.preventDefault event)
                                  (zrf/dispatch [toggle-opened opts]))
        on-container-key-down   (fn [event]
                                  (case (.-key event)
                                    "Escape" (do (zrf/dispatch [close opts]) (.preventDefault event))
                                    "Enter" (do (zrf/dispatch [select-active opts]) (.preventDefault event))
                                    "ArrowUp" (do (zrf/dispatch [activate-sibling opts -1]) (.preventDefault event))
                                    "ArrowDown" (do (zrf/dispatch [activate-sibling opts +1]) (.preventDefault event)) nil))
        on-clear-mouse-down     (fn [event]
                                  (.stopPropagation event)
                                  (.preventDefault event)
                                  (zrf/dispatch [select opts clear-value]))
        on-active-option-ref    (fn [ref]
                                  (when (and ref @should-scroll)
                                    (scroll-into-view ref)
                                    (zrf/dispatch [set-should-scroll opts false])))
        on-option-mouse-down    (fn [option event]
                                  (.preventDefault event)
                                  (zrf/dispatch [select opts option]))
        on-option-mouse-move    (fn [option _]
                                  (zrf/dispatch [activate opts option]))]

    (fn [props]
      (let [search-value    @search-value
            opened          @opened
            value           @value
            active          @active
            current-options @current-options
            loading         @loading
            clearable       (:clearable props true)
            disabled        (boolean (:disabled props))
            searchable      (boolean (:searchable props true))

            {:keys [id-fn] :or {id-fn default-id-fn}} @schema

            render-value    (:render-value props str)
            render-option   (:render-option props render-value)]
        [:div {:class         [base-class (class-names (:class props))]
               :disabled      disabled
               :on-mouse-down (when-not disabled on-container-mouse-down)
               :on-key-down   (when-not disabled on-container-key-down)}

         (when (or (not searchable) (not opened))
           [:div {:class [value-class
                          (when (seq search-value) (c [:invisible]))
                          (when (empty? value) (c [:text :gray-500]))]}
            (if value
              (render-value value)
              (:placeholder props))])

         [:input {:class           [search-class
                                    (class-names (:search-class props))
                                    (when-not opened (c [:opacity 0]))]
                  :id              (zf/get-id opts)
                  :disabled        disabled
                  :type            "search"
                  :value           search-value
                  :read-only       (not searchable)
                  :on-change       #(zrf/dispatch [search opts (.. % -target -value)])
                  :on-blur         #(zrf/dispatch [close opts])
                  :placeholder     (when (and opened searchable) "Search...")
                  :auto-capitalize "none"
                  :auto-complete   "off"
                  :auto-correct    "off"
                  :spell-check     "false"}]

         [:div {:class    icon-class
                :disabled disabled}
          (cond
            loading
            [:i.far.fa-spinner-third.fa-spin]

            (and clearable (some? value))
            [:i.fal.fa-times {:class         (c :cursor-pointer)
                              :on-mouse-down on-clear-mouse-down}]

            :else
            [:i.far.fa-chevron-down {:class (c :text-xs)}])]

         (when opened
           [:div {:class [dropdown-class (class-names (:dropdown-class props))]}
            (if (seq current-options)
              (for [option current-options :let [active?   (= (id-fn option) (id-fn active))
                                                 selected? (= (id-fn option) (id-fn value))]]
                [:div {:class         [option-class (cond
                                                      selected? selected-option-class
                                                      active? active-option-class)]
                       :key           (str (id-fn option))
                       :ref           (when active? on-active-option-ref)
                       :on-mouse-down (partial on-option-mouse-down option)
                       :on-mouse-move (when-not active? (partial on-option-mouse-move option))}
                 (render-option option)])
              [:div {:class empty-class} "No option"])])]))))


(defmethod value-fn
  ::state-resource
  [_ option]
  {:id           (:value option)
   :resourceType "State"
   :display      (:display option)})


(zrf/reg-event-db
  init-demo
  (fn [db _]
    (let [schema
          {:id-fn    :value
           :value-fn ::state-resource
           :options  [{:value "alabama" :display "Alabama"}
                      {:value "alaska" :display "Alaska"}
                      {:value "arizona" :display "Arizona"}
                      {:value "arkansas" :display "Arkansas"}
                      {:value "california" :display "California"}
                      {:value "colorado" :display "Colorado"}
                      {:value "connecticut" :display "Connecticut"}
                      {:value "delaware" :display "Delaware"}
                      {:value "florida" :display "Florida"}
                      {:value "georgia" :display "Georgia"}
                      {:value "hawaii" :display "Hawaii"}
                      {:value "idaho" :display "Idaho"}
                      {:value "illinois" :display "Illinois"}
                      {:value "indiana" :display "Indiana"}
                      {:value "iowa" :display "Iowa"}
                      {:value "kansas" :display "Kansas"}
                      {:value "kentucky" :display "Kentucky"}
                      {:value "louisiana" :display "Louisiana"}
                      {:value "maine" :display "Maine"}
                      {:value "maryland" :display "Maryland"}
                      {:value "massachusetts" :display "Massachusetts"}
                      {:value "michigan" :display "Michigan"}
                      {:value "minnesota" :display "Minnesota"}
                      {:value "mississippi" :display "Mississippi"}
                      {:value "missouri" :display "Missouri"}
                      {:value "montana" :display "Montana"}
                      {:value "nebraska" :display "Nebraska"}
                      {:value "nevada" :display "Nevada"}
                      {:value "new-hampshire" :display "New Hampshire"}
                      {:value "new-jersey" :display "New Jersey"}
                      {:value "new-mexico" :display "New Mexico"}
                      {:value "new-york" :display "New York"}
                      {:value "north-carolina" :display "North Carolina"}
                      {:value "north-dakota" :display "North Dakota"}
                      {:value "ohio" :display "Ohio"}
                      {:value "oklahoma" :display "Oklahoma"}
                      {:value "oregon" :display "Oregon"}
                      {:value "pennsylvania" :display "Pennsylvania"}
                      {:value "rhode-island" :display "Rhode Island"}
                      {:value "south-carolina" :display "South Carolina"}
                      {:value "south-dakota" :display "South Dakota"}
                      {:value "tennessee" :display "Tennessee"}
                      {:value "texas" :display "Texas"}
                      {:value "utah" :display "Utah"}
                      {:value "vermont" :display "Vermont"}
                      {:value "virginia" :display "Virginia"}
                      {:value "washington" :display "Washington"}
                      {:value "west-virginia" :display "West Virginia"}
                      {:value "wisconsin" :display "Wisconsin"}
                      {:value "wyoming" :display "Wyoming"}]}]
      (-> db
          (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:select :one]})
                    {:options ["Apple" "Banana" "Orange"]})
          (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:select :two]}) schema)
          (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:select :three]}) schema)
          (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:select :four]}) schema)
          (assoc-in (zf/state-path {:zf/root [::db] :zf/path [:select :three]}) {:loading true})
          (assoc-in (zf/value-path {:zf/root [::db] :zf/path [:select :four]}) {:value "oklahoma" :display "Oklahoma"})
          (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:select :five]})
                    {:id-fn :id
                     :http  {:uri    "/healthplans/hplookup"
                             :params {:limit 15}}})
          (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:select :sixth]}) schema)))))


(defn demo
  []
  (zrf/dispatch [init-demo])
  (fn []
    [block {:title "Select"}
     [zf-select {:placeholder "Default"
                 :opts        {:zf/root [::db] :zf/path [:select :one]}}]
     [zf-select {:placeholder  "Disabled"
                 :opts         {:zf/root [::db] :zf/path [:select :two]}
                 :render-value :display :disabled true}]
     [zf-select {:placeholder  "Loading"
                 :opts         {:zf/root [::db] :zf/path [:select :three]}
                 :render-value :display}]
     [zf-select {:placeholder  "Unclearable"
                 :opts         {:zf/root [::db] :zf/path [:select :four]}
                 :render-value :display :clearable false}]
     [zf-select {:placeholder    "Default"
                 :opts           {:zf/root [::db] :zf/path [:select :five]}
                 :dropdown-class (c [:w 80])
                 :render-value   :name
                 :render-option  (fn [health-plan]
                                   [:div {:class (c [:space-x 1])}
                                    [:strong (:id health-plan)]
                                    [:span (:name health-plan)]])}]
     [zf-select {:placeholder  "Unsearchable"
                 :opts         {:zf/root [::db] :zf/path [:select :sixth]}
                 :render-value :display :searchable false}]]))

