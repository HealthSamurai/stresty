(ns anti.mask-input
  (:require
    [anti.input]
    [anti.util :refer [class-names block track after-render current-timestamp]]
    [input-mask.core :as mask]
    [stylo.core :refer [c c?]]
    [zf.core :as zf]
    [zframes.re-frame :as zrf]))


(defn get-mask
  [db opts]
  (or (zf/state db opts [:mask])
      (mask/make-input-mask
        (select-keys
          (zf/schema db opts)
          [:pattern :format-characters
           :placeholder-char
           :revealing?]))))


(zrf/reg-event-fx init
  (fn [{db :db} [_ opts]]
    (when (zf/schema db opts)
      {:db (zf/set-state db opts {:mask (get-mask db opts)})})))


(zrf/reg-event-fx input
  (fn [{db :db} [_ opts selection char]]
    (let [mask (get-mask db opts)
          mask (mask/set-value mask (zf/value db opts))
          mask (assoc mask :selection selection)
          mask (or (mask/input mask char) mask)]
      {:db (-> db
               (zf/set-state opts [:mask] mask)
               (zf/set-value opts (mask/get-value mask)))})))


(zrf/reg-event-fx backspace
  (fn [{db :db} [_ opts selection]]
    (let [mask (get-mask db opts)
          mask (mask/set-value mask (zf/value db opts))
          mask (assoc mask :selection selection)
          mask (or (mask/backspace mask) mask)]
      {:db (-> db
               (zf/set-state opts [:mask] mask)
               (zf/set-value opts (mask/get-value mask)))})))


(zrf/reg-sub
  mask
  (fn [[_ opts]]
    (zrf/subscribe [::zf/state opts]))
  (fn [state _]
    (:mask state)))


(zrf/reg-sub
  empty-value
  (fn [[_ opts]]
    (zrf/subscribe [mask opts]))
  (fn [mask _]
    (:empty-value mask)))


(zrf/reg-sub
  selection
  (fn [[_ opts]]
    (zrf/subscribe [mask opts]))
  (fn [mask _]
    (:selection mask)))


(zrf/reg-sub
  value
  (fn [[_ opts]]
    [(zrf/subscribe [::zf/value opts])
     (zrf/subscribe [mask opts])])
  (fn [[value mask] _]
    (or value
        (let [value (mask/get-value mask)]
          (if (= value (:empty-value mask))
            "" value)))))


(zrf/reg-sub
  length
  (fn [[_ opts]]
    (zrf/subscribe [mask opts]))
  (fn [mask _]
    (get-in mask [:pattern :length])))


(defn get-selection
  [el]
  {:start (.-selectionStart el)
   :end   (.-selectionEnd el)})


(defn set-selection
  [el selection]
  (let [before-ts (:ts @selection)]
    (after-render
      (fn []
        (when-let [{:keys [start end ts]} @selection]
          (when (= ts before-ts)
            (set! (.-selectionStart el) start)
            (set! (.-selectionEnd el) end)
            (vreset! selection nil)))))))


(defn zf-mask-input
  [{:keys [opts]}]
  (zrf/dispatch [init opts])
  (let [selection-c  (volatile! nil)
        value        (zrf/subscribe [value opts])
        empty-value  (zrf/subscribe [empty-value opts])
        length       (zrf/subscribe [length opts])
        selection    (zrf/subscribe [selection opts])
        on-key-press (fn [event]
                       (when-not (or (.-metaKey event) (.-altKey event) (.-ctrlKey event) (.-metaKey event) (= "Enter" (.-key event)))
                         (.preventDefault event)
                         (zrf/dispatch-sync [input opts (or @selection-c (get-selection (.-target event))) (.-key event)])
                         (vreset! selection-c (assoc @selection :ts (current-timestamp)))
                         (set-selection (.-target event) selection-c)))
        on-key-down  (fn [event]
                       (when (= "Backspace" (.-key event))
                         (.preventDefault event)
                         (zrf/dispatch-sync [backspace opts (or @selection-c (get-selection (.-target event)))])
                         (vreset! selection-c (assoc @selection :ts (current-timestamp)))
                         (set-selection (.-target event) selection-c)))]
    (fn [props]
      (let [value       @value
            empty-value @empty-value
            length      @length]
        [anti.input/input
         (merge (dissoc props :opts)
                {:value        value
                 :size         length
                 :on-change    identity
                 :on-key-press on-key-press
                 :on-key-down  on-key-down
                 :placeholder  (or (:placeholder props) empty-value)})]))))


(zrf/reg-event-db
  init-demo
  (fn [db _]
    (-> db
        (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:mask-input :one]})
                  {:pattern "11/11/1111"})
        (assoc-in (zf/schema-path {:zf/root [::db] :zf/path [:mask-input :two]})
                  {:pattern "11/11/1111" :revealing? true}))))


(defn demo
  []
  (zrf/dispatch [init-demo])
  (fn []
    [block {:title "Masked input"}
     [zf-mask-input
      {:opts {:zf/root [::db] :zf/path [:mask-input :one]}}]
     [zf-mask-input
      {:opts        {:zf/root [::db] :zf/path [:mask-input :two]}
       :placeholder "Revealing"}]]))
