(ns anti.dropdown-select-model
  (:require [re-frame.core :as rf]
            [zf.core :as zf]
            [clojure.string :as str]))

(rf/reg-sub
  :anti/dropdown-select-options
  (fn [[_ opts]]
    [(rf/subscribe [:zf.core/schema opts])
     (rf/subscribe [:zf.core/state opts])])

  (fn [[schema state] _]
    (->> (or (:options state) (:options schema))
         (map-indexed (fn [i option]
                        (assoc option :anti/selected (= i (:selection state))))))))


(rf/reg-event-fx
  :anti/dropdown-select-move-selection
  (fn [{db :db} [_ opts step]]
    (let [state     (zf/state db opts)
          selection (mod (+ step (or (:selection state) 0)) (count (:options state)))]
      {:db        (zf/set-state db opts [:selection] selection)
       :into-view (:id (nth (:options state) selection nil))})))


(defn filter-opts [opts q]
  (cond->> opts
    (and q (not (str/blank? q)))
    (filter #(str/index-of (str/lower-case (:display %)) (str/lower-case q)))))


(rf/reg-event-fx
  :anti/dropdown-select-search
  (fn [{db :db} [_ opts q]]
    (let [sch (zf/schema db opts)]
      (if-let [uri (:uri sch)]
        {:db         (zf/merge-state db opts {:search q :loading true :selection 0})
         :http/fetch {:uri      uri
                      :params   (merge (:params sch) {(:q sch) q})
                      :unbundle true
                      :as       :options
                      :map-f    (fn [i] {:id (:id i) :value (:id i) :display (:name i)})
                      :path     (zf/state-path opts)}}
        {:db (zf/merge-state db opts {:search    q
                                      :options   (filter-opts (:options sch) q)
                                      :selection 0})}))))


(rf/reg-event-db
  :anti/dropdown-select-open
  (fn [db [_ opts]]
    (let [sch (zf/schema db opts)]
      (zf/merge-state db opts (cond-> {:open true :search nil :selection 0}
                                (not (:uri sch))
                                (assoc :options (:options sch)))))))


(rf/reg-event-fx
  :anti/dropdown-select-close
  (fn [{db :db} [_ opts]]
    {:db    (zf/merge-state db opts {:open false :search nil})
     :focus (:id opts)}))


(rf/reg-event-fx
 :anti/dropdown-datepicker-close
 (fn [{db :db} [_ opts]]
   (let [state (zf/state db opts)]
     {:db (if-not (:date-open state)
            (zf/merge-state db opts {:open false :search nil})
            db)})))


(rf/reg-event-fx
  :anti/dropdown-select-pick
  (fn [{db :db} [_ opts v]]
    ;; todo write helper for on-chnage
    ;; form engine should fire on-change from model
    (let [{:keys [on-change]} (zf/schema db opts)
          evs (cond-> [[:zf.core/set-value opts v]]
                on-change
                (conj [(:event on-change) (assoc on-change :opts opts :value v)]))]
      (cond-> {:db (zf/set-state db opts [:open] false)
               :focus (:id opts)
               :dispatch-n evs}))))


(rf/reg-event-fx
  :anti/dropdown-select-pick-selection
  (fn [{db :db} [_ opts]]
    (let [state (zf/state db opts)]
      {:dispatch [:anti/dropdown-select-pick opts
                  (nth (:options state) (or (:selection state) 0) nil)]})))


(rf/reg-event-fx
 :anti/dropdown-datepicker-open
 (fn [{db :db} [_ opts]]
   (let [state (zf/state db opts)]
     {:db (zf/set-state db opts [:date-open] (not (:date-open state)))})))


(rf/reg-event-fx
 :anti/dropdown-datepicker-pick
 (fn [{db :db} [_ opts v]]
   (let [{:keys [on-change]} (zf/schema db opts)
         v {:value v}
         evs (cond-> [[:zf.core/set-value opts v]]
               on-change
               (conj [(:event on-change) (assoc on-change :opts opts :value v)]))]
     {:db (zf/set-state db opts [:date-open] false)
      :dispatch-n evs})))

