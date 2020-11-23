(ns anti.radio-model
  (:require [re-frame.core :as rf]
            [zf.core :as zf]
            [clojure.string :as str]))

(rf/reg-event-fx
 :anti/radiogroup-pick
 (fn [{db :db} [_ opts v]]
   (let [state (zf/state db opts)
         {on-change-event :on-change :as sch} (zf/schema db opts)
         assign-id (fn [opt]
                     (assoc opt :id (or (:id opt) (str/join "." (conj (mapv name (concat (:zf/root opts) (:zf/path opts))) (:value opt))))))
         new-opts (mapv assign-id (:options sch))]
     (when (or (:onChange opts) (:on-change opts))
       (if-let [foo (:on-change opts)]
         (foo v)
         ((:onChange opts) v)))
     (cond-> {:db (-> db
                     (zf/set-value opts v)
                     (zf/merge-state opts {:selection (:value v)
                                           :options new-opts}))}
       on-change-event (assoc :dispatch
                              [(:event on-change-event)
                               (assoc on-change-event :opts opts :value v)])))))

(rf/reg-event-fx
 :anti/radiogroup-navigate
 (fn [{db :db} [_ opts direction]]
   (let [state (zf/state db opts)
         sch (zf/schema db opts)
         new-selection (+ direction (or (:selection state) 0))
         opts-count (count (:options state))
         new-selection (if (< new-selection 1)
                         opts-count
                         (if (> new-selection opts-count)
                           1 new-selection))
         opt (nth (:options state) (dec new-selection) nil)]
     (if-not state
       (rf/dispatch [:anti/radiogroup-pick opts (first (:options sch))])
       {:db (-> db
                (zf/set-state opts [:selection] new-selection)
                (zf/set-value opts opt))}))))

(rf/reg-event-db
 :anti/checkbox-group-toggle
 (fn [db [_ opts v]]
   (let [sch (zf/schema db opts)
         val (if-let [val (zf/value db opts)]
               val
               (vec (repeat (count (:options sch)) false)))
         new-val (assoc val (dec (:value v)) (not (nth val (dec (:value v)))))]
     (-> db
         (zf/set-value opts new-val)
         (zf/set-state opts [:selection] (:value v))))))

(rf/reg-event-fx
 :anti/checkbox-group-navigate
 (fn [{db :db} [_ opts direction]]
   (let [state (zf/state db opts)
         sch (zf/schema db opts)
         assign-id (fn [opt]
                     (assoc opt :id (or (:id opt) (str/join "." (conj (mapv name (concat (:zf/root opts) (:zf/path opts))) (:value opt))))))
         new-opts (mapv assign-id (:options sch))
         new-selection (+ direction (or (:selection state) 0))
         opts-count (count (:options sch))
         new-selection (if (< new-selection 1)
                         opts-count
                         (if (> new-selection opts-count)
                           1 new-selection))
         opt (nth new-opts (dec new-selection) nil)]
     {:db (-> db
              (zf/set-state opts [:selection] new-selection))
      :focus (:id opt)})))
