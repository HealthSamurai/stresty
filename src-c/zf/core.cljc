(ns zf.core
  (:require
    [re-frame.core :as rf]
    [clojure.string :as str]))

(defmulti validate (fn [cfg value] (:type cfg)))

(defn assoc-in*
  [m [k & ks] v]
  (let [assoc
        (fn [m k v]
          (if (and (int? k)
                   (or (nil? m) (vector? m))
                   (>= k (count m)))
            (assoc (into (or m []) (repeat (- k (count m)) nil)) k v)
            (assoc m k v)))]
    (if ks
      (assoc m k (assoc-in* (get m k) ks v))
      (assoc m k v))))


(defn get-id** [opts & subpath]
  (str/replace
    (->> (concat (:zf/root opts) (:zf/path opts) subpath)
         (mapv str)
         (str/join "."))
    #"\W+" "_"))

(def get-id (memoize get-id**))


(defn with-path
  [opts & args]
  (apply update opts :zf/path into args))


(defn state-path
  ([{:zf/keys [root path]}]
   (concat root [:state] path))
  ([{:zf/keys [root path]} inner-path]
   (concat root [:state] path inner-path)))

(defn schema-path
  ([{:zf/keys [root path]}]
   (concat root [:schema] path))
  ([{:zf/keys [root path]} inner-path]
   (concat root [:schema] path inner-path)))

(defn value-path [{:zf/keys [root path]}]
  (concat root [:value] path))

(defn error-path [{:zf/keys [root path]}]
  (concat root [:error] path))

(defn set-value
  [db opts value]
  (let [value (if (and value (string? value) (str/blank? value)) nil value)]
    (assoc-in* db (value-path opts) value)))

(defn set-error
  [db opts error]
  (assoc-in* db (error-path opts) error))

(rf/reg-event-fx
  ::set-error
  (fn [{db :db} [_ opts error]]
    {:db (set-error db opts error)}))

(defn merge-state
  [db {root :zf/root path :zf/path} value]
  (update-in db (concat root [:state] path) merge value))

(rf/reg-event-db
  ::merge-state
  (fn [db [_ opts value]]
    (merge-state db opts value)))


(defn set-state
  ([db opts value]
   (assoc-in db (state-path opts) value))
  ([db opts inner-path value]
   (assoc-in db (state-path opts inner-path) value)))

(rf/reg-event-db
  ::set-state
  (fn [db [_ opts inner-path value]]
    (set-state db opts inner-path value)))


(defn schema
  [db opts]
  (get-in db (schema-path opts)))

(rf/reg-sub
  ::schema
  (fn [db [_ opts]]
    (schema db opts)))


(defn value
  [db opts]
  (get-in db (value-path opts)))

(rf/reg-sub
  ::value
  (fn [db [_ opts]]
    (value db opts)))

(defn error
  [db opts]
  (get-in db (error-path opts)))

(rf/reg-sub
  ::error
  (fn [db [_ opts]]
    (error db opts)))

(defn state
  ([db opts]
   (get-in db (state-path opts)))
  ([db opts inner-path]
   (get-in db (state-path opts inner-path))))

(rf/reg-sub
  ::state
  (fn [db [_ opts]]
    (state db opts)))

(rf/reg-event-fx
  ::commit-value
  (fn [{db :db} [_ opts]]
    (let [opts (dissoc opts :zf/path)]
      (when-let [event (:on-commit (get-in db (:zf/root opts)))]
        (let [value           (value db (dissoc opts :zf/path))
              committed-value (state db opts [::committed-value])]
          (when-not (= value committed-value)
            {:db       (assoc-in db (state-path opts [::committed-value]) value)
             :dispatch (conj (if (vector? event) event [event]) value)}))))))

(rf/reg-event-fx
  ::set-value
  (fn [{db :db} [_ opts v flags]]
    (let [db (set-value db opts v)]
      {:db         db
       :dispatch-n [(when-let [event (:on-change (schema db opts))]
                      (into (if (vector? event) event [event]) [opts v]))
                    (when-let [event (:on-change (get-in db (:zf/root opts)))]
                      (into (if (vector? event) event [event]) [(value db (dissoc opts :zf/path)) opts v]))
                    (when (:auto-commit flags true)
                      [::commit-value opts])]})))
