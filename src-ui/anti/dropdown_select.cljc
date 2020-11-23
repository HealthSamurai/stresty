(ns anti.dropdown-select
  (:require
    [stylo.core :refer [c c?]]
    [anti.click-outside]
    [zf.core :as zf]
    [anti.input]
    [clojure.string :as str]
    [zframes.re-frame :as zrf]))


(def dropdown-class
  (c :absolute
     :rounded
     :leading-relaxed
     {:box-shadow "0 3px 6px -4px rgba(0,0,0,.12), 0 6px 16px 0 rgba(0,0,0,.08), 0 9px 28px 8px rgba(0,0,0,.05)"}
     :overflow-x-hidden
     :overflow-y-hidden
     [:bg :white]
     [:z 100]
     [:mt "4px"]
     [:top "100%"]
     [:left "-1px"]
     [:right "-1px"]
     [:w-min 40]))

(def option-class
  (c [:px 2] [:py 1]
     :truncate
     :cursor-default
     [:hover [:bg :gray-200]]))


(def selected-option-class
  (c [:bg :blue-100]))


(def empty-class
  (c [:px 2] [:py 2]
     :truncate
     :cursor-default
     :text-center
     [:text :gray-500]))


(zrf/reg-sub
  options
  (fn [[_ opts]]
    [(zrf/subscribe [::zf/schema opts])
     (zrf/subscribe [::zf/state opts])])

  (fn [[schema state] _]
    (->> (or (:options state) (:options schema))
         (map-indexed (fn [i option]
                        (assoc option ::selected (= i (:selection state))))))))


(zrf/reg-event-fx
  move-selection
  (fn [{db :db} [_ opts step]]
    (let [state     (zf/state db opts)
          selection (mod (+ step (or (:selection state) 0)) (count (:options state)))]
      {:db              (zf/set-state db opts [:selection] selection)
       :dom/scroll-into (:id (nth (:options state) selection nil))})))


(defn filter-opts [opts q]
  (cond->> opts
    (and q (not (str/blank? q)))
    (filter #(str/index-of (str/lower-case (:display %)) (str/lower-case q)))))


(zrf/reg-event-fx
  search-ok
  (fn [{db :db} [_ {opts :opts data :data}]]
    {:db (zf/merge-state db opts
                         {:loading false
                          :options (->> data
                                        (map (fn [i] {:id (:id i)
                                                      :value (:id i)
                                                      :display (:name i)})))})}))


(zrf/reg-event-fx
  search
  (fn [{db :db} [_ opts q]]
    (let [sch (zf/schema db opts)]
      (if-let [uri (:uri sch)]
        {:db         (zf/merge-state db opts {:search q :loading true :selection 0})
         :http/fetch {:uri      uri
                      :params   (merge (:params sch) {(:q sch) q})
                      :unbundle true
                      :success  {:event search-ok :opts opts}}}
        {:db (zf/merge-state db opts {:search    q
                                      :options   (filter-opts (:options sch) q)
                                      :selection 0})}))))


(zrf/reg-event-db
  open
  (fn [db [_ opts]]
    (let [sch (zf/schema db opts)]
      (zf/merge-state db opts (cond-> {:open true :search nil :selection 0}
                                (not (:uri sch))
                                (assoc :options (:options sch)))))))


(zrf/reg-event-fx
  close
  (fn [{db :db} [_ opts]]
    {:db        (zf/merge-state db opts {:open false :search nil :options nil})
     :dom/focus (zf/get-id opts)}))


(zrf/reg-event-fx
  pick
  (fn [{db :db} [_ opts v]]
    {:db        (zf/merge-state db opts {:open false :search nil :options nil})
     :dom/focus (zf/get-id opts)
     :dispatch  [::zf/set-value opts v]}))


(zrf/reg-event-fx
  pick-selection
  (fn [{db :db} [_ opts]]
    (let [state (zf/state db opts)]
      {:dispatch [pick opts
                  (nth (:options state) (or (:selection state) 0) nil)]})))


(defn dropdown-select
  [{:keys [opts display-fn]}]
  (let [state       (zrf/subscribe [::zf/state opts])
        display-fn  (if display-fn display-fn :display)
        options     (zrf/subscribe [options opts])
        searchable  (boolean (:searchable opts true))
        on-search   (fn [e] (zrf/dispatch [search opts (.. e -target -value)]))
        on-open     (fn [e] (zrf/dispatch [open opts]) (doto e .preventDefault .stopPropagation))
        on-close    (fn [_] (zrf/dispatch [close opts]))
        on-pick     (fn [v] (zrf/dispatch [pick opts v]))
        on-key-down (fn [e] (cond (or (= "ArrowUp" (.-key e)) (and (.-ctrlKey e) (= "k" (.-key e))))
                                  (do (zrf/dispatch [move-selection opts -1]) (.preventDefault e))
                                  (or (= "ArrowDown" (.-key e)) (and (.-ctrlKey e) (= "j" (.-key e))))
                                  (do (zrf/dispatch [move-selection opts +1]) (.preventDefault e))
                                  (= "Enter" (.-key e))
                                  (do (zrf/dispatch [pick-selection opts]) (.preventDefault e))
                                  (= "Escape" (.-key e))
                                  (do (zrf/dispatch [close opts]) (.preventDefault e))))]
    (fn [_ & children]
      (let [state @state options @options]
        [:div {:class (c :relative :inline-block)}
         (into [:div {:class            (c :inline-block :cursor-pointer)
                      :on-click-capture (if (:open state) on-close on-open)}] children)
         (when (:open state)
           [anti.click-outside/click-outside {:on-click on-close}
            [:div {:class dropdown-class}
             (when searchable
               [:div {:class (c :relative [:p 2])}
                [anti.input/input
                 {:placeholder "Search"
                  :prefix      [:i.fal.fa-search {:class (c [:text :gray-500] :pointer-events-none)}]
                  :on-key-down on-key-down
                  :auto-focus  true
                  :value       (:search state)
                  :on-change   on-search}]])
             (cond
               (:loading state) [:div {:class empty-class} "Loading..."]
               (empty? options) [:div {:class empty-class} "No option"]
               :else
               [:div {:class (c :overflow-y-scroll [:pb 1] [:h-max 60])}
                (for [opt options]
                  [:div
                   {:key      (:value opt)
                    :id       (:id opt)
                    :on-click #(on-pick opt)
                    :class    [option-class
                               (when (::selected opt) selected-option-class)]}
                   (or (display-fn opt) (str opt))])])]])]))))
