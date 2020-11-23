(ns anti.select-model
  (:require [re-frame.core :as rf]
            [zf.core :as zf]
            [clojure.string :as str]))

(defmulti value-fn (fn [k obj] k))
(defmethod value-fn
  :default [_ obj]
  obj)

(rf/reg-sub
  :anti/select-options
  (fn [[_ opts]]
    [(rf/subscribe [:zf.core/schema opts])
     (rf/subscribe [:zf.core/state opts])])

  (fn [[schema state] _]
    (->> (or (:options state) (:options schema))
         (map-indexed (fn [i option]
                        (assoc option :anti/selected (= i (:selection state))))))))


(rf/reg-event-fx
  :anti/select-move-selection
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
  :anti/select-search
  (fn [{db :db} [_ opts q]]
    (let [sch (zf/schema db opts)]
      (if-let [uri (:uri sch)]
        {:db         (zf/merge-state db opts {:search q :loading true :selection 0})
         :http/fetch {:uri      uri
                      :params   (merge (:params sch) {(:q sch) q})
                      :unbundle true
                      :debounce (or (:debounce sch) 300)
                      :as       :options
                      :path     (zf/state-path opts)}}
        {:db (zf/merge-state db opts {:search    q
                                      :options   (filter-opts (:options sch) q)
                                      :selection 0})}))))


(rf/reg-event-db
  :anti/select-open
  (fn [db [_ opts]]
    (let [sch (zf/schema db opts)]
      (zf/merge-state db opts (cond-> {:open true :search nil :selection 0}
                                (not (:uri sch))
                                (assoc :options (:options sch)))))))


(rf/reg-event-fx
  :anti/select-close
  (fn [{db :db} [_ opts]]
    {:db    (zf/merge-state db opts {:open false :search nil})
     :focus (:id opts)}))


(rf/reg-event-fx
 :anti/select-pick
 (fn [{db :db} [_ opts v]]
   (let [sch (zf/schema db opts)
         {vk :anti.select/value-fn on-change :on-change} sch
         v* (if vk
             (value-fn vk v)
             v)]
     (cond-> {:db    (-> db
                        (zf/set-value opts v*)
                        (zf/set-state opts [:open] false))
              :focus (:id opts)}
       on-change (assoc :dispatch
                        [(:event on-change)
                         (assoc on-change :opts opts :value v* :raw-value v)])))))

(rf/reg-event-fx
  :anti/select-clear
  (fn [{db :db} [_ opts]]
    {:db    (-> db
                (zf/set-value opts nil)
                (zf/set-state opts [:open] false))
     :focus (:id opts)}))


(rf/reg-event-fx
  :anti/select-selection
  (fn [{db :db} [_ opts]]
    (let [state (zf/state db opts)
          sch (zf/schema db opts)
          {vk :anti.select/value-fn on-change :on-change} sch
          v (nth (:options state) (or (:selection state) 0) nil)
          v* (if vk (value-fn vk v) v)]
      (cond-> {:db    (-> db
                         (zf/set-value opts v*)
                         (zf/set-state opts [:open] false))
               :focus (:id opts)}
        on-change (assoc :dispatch
                         [(:event on-change)
                          (assoc on-change :opts opts :value v* :raw-value v)])))))
