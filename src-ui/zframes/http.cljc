(ns zframes.http
  (:require [clojure.string :as str]
            [zframes.routing :refer [unparse-query-string]]
            [re-frame.db :refer [app-db]]
            [zframes.re-frame :as zrf]))


(defn extract [unbundle? resp]
  (if (and unbundle? (or (:entry resp) (= "Bundle" (:resourceType resp))))
    (let [data (map :resource (:entry resp))]
      {:data data
       :total (:total resp)})
    {:data resp}))


(defn make-event
  [opts type payload]
  (when-let [event (get opts type)]
    [(:event event) (merge event {:request opts} payload)]))


(zrf/defx fetch-start
  [{db :db} [_ {path :path}]]
  {:db (cond-> db path (assoc-in (conj path :loading) true))})


(zrf/defx http-ok
  [{db :db} [_ {path :path unbundle :unbundle :as opts} resp doc]]
  {:dispatch-n [(make-event opts :success (merge {:response resp} (extract unbundle doc)))]
   :db (cond-> db path (update-in path merge (merge (extract unbundle doc) {:loading false :error nil})))})


(zrf/defx http-error
  [{db :db} [_ {path :path :as opts} resp doc]]
  {:dispatch-n [(make-event opts :error {:response resp :data doc})]
   :db (cond-> db path (update-in path merge {:loading false :data nil :error doc}))})


(zrf/defx json-error
  [{db :db} [_ {path :path :as opts} resp error]]
  {:dispatch-n [(make-event opts :error {:response resp :data error})]
   :db (cond-> db path (update-in path merge {:loading false :data nil :error error}))})


(zrf/defx fetch-error
  [{db :db} [_ {path :path :as opts} error]]
  {:console/error [error]
   :dispatch-n [(make-event opts :error {:error error})]
   :db (cond-> db path (update-in path merge {:loading false :data nil :error {:err error}}))})


(zrf/defe :http/fetch
  [opts]
  #?(:cljs
     (doseq [o (if (vector? opts) opts [opts])]
       (when-let [{:keys [uri format headers params files body] :as opts} o]

         (let [format (case format
                        "json" "application/json"
                        "yaml" "text/yaml"
                        "application/json")

               headers (cond-> {"accept" format}
                               (nil? files) (assoc "Content-Type" format
                                                   "Cache-Control" "no-cache")
                               true (merge headers))

               init (-> (merge {:method "get" :mode "cors"} opts)
                        (dissoc :uri :headers :success :error :params :files)
                        (assoc :headers headers)
                        (assoc :cache "no-store")
                        (cond-> body (assoc :body (if (string? body) body (js/JSON.stringify (clj->js body))))
                                files (assoc :body files)))]

           (zrf/dispatch [fetch-start opts])

           (-> (js/fetch (str uri (unparse-query-string params)) (clj->js init))
               (.then (fn [resp]
                        (if (some-> (.-headers resp)
                                    (.get "content-type")
                                    (str/index-of "application/json"))
                          (-> (.json resp)
                              (.then (fn [doc]
                                       (let [doc (js->clj doc :keywordize-keys true)]
                                         (if (< (.-status resp) 300)
                                           (zrf/dispatch [http-ok opts resp doc])
                                           (zrf/dispatch [http-error opts resp doc]))))
                                     (fn [error]
                                       (let [error (js->clj error :keywordize-keys true)]
                                         (zrf/dispatch [json-error opts resp error])))))
                          (-> (.text resp)
                              (.then (fn [doc]
                                       (if (< (.-status resp) 300)
                                         (zrf/dispatch [http-ok opts resp doc])
                                         (zrf/dispatch [http-error opts resp doc]))))))))
               (.catch (fn [error]
                         (zrf/dispatch [fetch-error opts error])))))))))
