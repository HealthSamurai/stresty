(ns anti.tags-list
  (:require
    [stylo.core :refer [c]]
    [anti.util :refer [block class-names]]
    [anti.input]
    [zframes.re-frame :as zrf]
    [clojure.string :as str]
    [zf.core :as zf]))


(def tag-class
  (c [:mb 2] [:mr 2] :border :rounded [:pl 2] :flex :items-center))


(def input-class
  (c :leading-normal :border [:px 2] [:mb 2] [:mr 2] [:w 31]))


(def remove-class
  (c :text-xs [:px 2] [:pt 0.5] :self-stretch :flex :items-center
     [:text :gray-500]
     [:hover :cursor-pointer [:text :gray-900]]
     {:transition-duration "0.2s"}))


(def clear-class
  (c :inline-flex
     :items-center
     [:mb 2] [:mr 2]
     [:leading-normal]
     [:text :gray-500]
     [:space-x 1]
     [:px 2]
     :border
     :rounded
     [:bg :white]
     :transition-all [:duration 200] :ease-in-out
     [:focus :outline-none :shadow-outline]
     [:pseudo ":not(:disabled)"
      [:hover [:text :blue-500] [:border :blue-500]]
      [:active [:text :blue-800] [:border :blue-800]]]
     [:disabled [:text :gray-500] [:bg :gray-200] [:border :gray-400] :cursor-not-allowed]))


(zrf/reg-event-fx delete-tag
  (fn [{db :db} [_ opts tag]]
    {:dispatch [::zf/set-value opts (filterv (fn [v] (not= v tag)) (zf/value db opts))]}))


(zrf/reg-event-fx clear-tags
  (fn [_ [_ opts]]
    {:dispatch [::zf/set-value opts nil]}))


(zrf/reg-event-fx add-tag
  (fn [{db :db} [_ opts tag]]
    (let [old-value (or (zf/value db opts) [])
          new-value (conj old-value tag)]
      (when (and (not (str/blank? tag))
                 (not (contains? (set old-value) tag)))
        {:dispatch [::zf/set-value opts new-value]}))))


(defn zf-tags-list
  [{:keys [completion-sub opts clearable] :as props}]
  (let [values      @(zrf/subscribe [::zf/value opts])
        datalist-id (zf/get-id opts ["datalist"])
        on-clear    (fn [_] (zrf/dispatch [clear-tags opts]))
        on-click    (fn [tag] (zrf/dispatch [delete-tag opts tag]))
        on-key-down (fn [event]
                      (when (or (= (.-key event) "Enter")
                                (= (.-key event) ",")
                                (= (.-key event) " "))
                        (.preventDefault event)
                        (.stopPropagation event)
                        (zrf/dispatch [add-tag opts (.. event -target -value)])
                        (aset (.. event -target) "value" "")))]

    [:div {:class (c :flex [:h-min 8] :items-center)}
     [:div {:class (c :flex :flex-wrap :items-center [:mb -2] [:mr -2])}
      (->> values
           (map-indexed
             (fn [i tag]
               [:div {:key   i
                      :class [tag-class (class-names (:tag-class props))]}
                [:span {:class (c :leading-normal)} tag]
                [:i.fal.fa-times {:on-click #(on-click tag)
                                  :class    remove-class}]])))
      (when completion-sub
        [:datalist {:id datalist-id}
         (->> @(zrf/subscribe [completion-sub])
              (map-indexed (fn [idx i] [:option {:key idx :value i}])))])
      [:input {:placeholder "+ New tag"
               :on-key-down on-key-down
               :id (zf/get-id opts)
               :list        (when completion-sub datalist-id)
               :class       [anti.input/base-class input-class]}]
      (when (and clearable (pos? (count values)))
        [:button {:type     "button"
                  :class    clear-class
                  :on-click on-clear}
         [:i.far.fa-trash {:class (c :transform [:scale 85])}] [:span "Remove all"]])]]))


(zrf/reg-event-db
  init-demo
  (fn [db _]
    (-> db
        (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:tags-list]})
                  {:options [{:value "new" :display "New"}
                             {:value "hold" :display "Hold"}
                             {:value "ready" :display "Ready"}
                             {:value "cancel" :display "Cancel"}]})
        (assoc-in (zf/value-path {:zf/root [::db] :zf/path [:tags-list]})
                  ["red" "green" "blue"]))))

(defn demo
  []
  (zrf/dispatch [init-demo])
  (fn []
    [block {:title "Tags list" :width "36rem"}
     [zf-tags-list {:clearable true
                    :opts         {:zf/root [::db]
                                   :zf/path [:tags-list]}}]]))
