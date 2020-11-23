(ns zframes.routing
  (:require
    [clojure.string :as str]
    [zframes.re-frame :as zrf]
    [route-map.core :as route-map]
    [re-frame.db :refer [app-db]]
    #?(:clj [ring.util.codec :as ring.codec])
    [medley.core :as medley]))


(defn url-encode [x]
  #?(:clj (ring.codec/url-encode x)
     :cljs (js/encodeURIComponent x)))

(defn url-decode [x]
  #?(:clj (ring.codec/url-decode x)
     :cljs (js/decodeURIComponent x)))


(defn unparse-query-string [params]
  (some->>
    params
    (medley/remove-vals #(nil? (if (seqable? %) (not-empty %) %)))
    (not-empty)
    (mapcat (fn [[k v]]
              (cond
                (vector? v) (mapv #(str (name k) "=" %) v)
                (set? v) [(str (name k) "=" (str/join "," (map (comp url-encode str) v)))]
                :else [(str (name k) "=" (url-encode (str v)))])))
    (str/join "&")
    (str "?")))


(defn parse-query-string [s]
  (when-let [s (not-empty (str/replace (or s "") #"^\?" ""))]
    (->> (str/split s #"&")
         (map #(str/split % #"=" 2))
         (group-by first)
         (medley/map-kv
           (fn [k pairs]
             [(keyword k)
              (let [vs (mapv (comp url-decode second) pairs)]
                (if (< 1 (count vs))
                  vs
                  (first vs)))])))))


(comment
  (unparse-query-string  {:a ["1" "2"] :b ["3" "1"]})
  (parse-query-string "?a=1&a=2&b=3&b=1"))


(defn global-context-events
  [global-contexts old-contexts new-contexts]
  (concat
    (reduce (fn [events [old-context old-params]]
              (let [new-params (get new-contexts old-context)]
                (if (and (not= new-params old-params)
                         (get global-contexts old-context))
                  (conj events [(get global-contexts old-context) :deinit old-params])
                  events)))
            [] old-contexts)
    (reduce (fn [events [new-context new-params]]
              (let [old-params (get old-contexts new-context)]
                (if (and (not= old-params new-params)
                         (get global-contexts new-context))
                  (conj events [(get global-contexts new-context) :init new-params])
                  events)))
            [] new-contexts)))


(zrf/defx search-changed
  [{db :db} [_ search]]
  (let [params (parse-query-string search)]
    (when-not (= (:global-params db) params)
      {:db (assoc db :global-params params
                     :global-query-string search)
       :dispatch-n (global-context-events
                     (get-in db [::db :global-contexts])
                     (:global-params db) params)})))


(defn parse-fragment [fragment]
  (let [[path query-string]
        (-> fragment
            (str/replace #"^#" "")
            (str/split #"\?" 2))]
    {:path path
     :query-string query-string
     :query-params (parse-query-string query-string)}))


(defn context-events
  [route old-contexts new-contexts]
  (concat
    (reduce (fn [events [old-context old-params]]
              (let [new-params (get new-contexts old-context)]
                (if (= new-params old-params)
                  events
                  (conj events [old-context :deinit old-params]))))
            [] old-contexts)
    (reduce (fn [events [new-context new-params]]
              (let [old-params (get old-contexts new-context)]
                (cond
                  (or (nil? old-params) (not= new-params old-params))
                  (conj events [new-context :init new-params])
                  (and old-params (= (:. new-params) route))
                  (conj events [new-context :return new-params])
                  :else events)))
            [] new-contexts)))


(defn page-events
  [old-route new-route]
  (if (= (:match old-route) (:match new-route))

    ;; same route
    (cond

      ;; nothing changed
      (= (:params old-route) (:params new-route))
      []

      ;; query-params changed
      (= (dissoc (:params old-route) :params) (dissoc (:params new-route) :params))
      [[(:match new-route) :params (:params new-route) (:params old-route)]]

      ;; route-params changed
      :else
      [[(:match new-route) :deinit (:params old-route)]
       [(:match new-route) :init (:params new-route)]])

    ;; different route
    (cond-> []
      ;; has old route
      (:match old-route)
      (conj [(:match old-route) :deinit (:params old-route)])
      ;; has new route
      true
      (conj [(:match new-route) :init (:params new-route)]))))


(defn collect-contexts
  [new-route]
  (reduce
    (fn [acc {params :params context :context route :.}]
      (if context
        (assoc acc context (assoc params :. route))
        acc))
    {} (:parents new-route)))


(zrf/defx fragment-changed
  [{db :db} [_ fragment]]
  (let [{:keys [path query-params query-string]} (parse-fragment fragment)]
    (if-let [new-route (some-> (route-map/match [:. path] (get-in db [::db :routes]))
                               (assoc-in [:params :params] query-params))]

      (let [new-contexts (collect-contexts new-route)]
        {:db (-> db
                 (assoc
                   :fragment fragment
                   :fragment-path path
                   :fragment-params (:params new-route)
                   :fragment-query-string query-string)
                 (update-in [::db :history] #(conj (take 4 %) {:route (:match new-route) :uri fragment}))
                 (assoc-in [::db :contexts] new-contexts)
                 (assoc-in [::db :route] new-route)
                 (assoc-in [::db :not-found?] false))
         :window/scroll-to [0 0]
         :dispatch-n (concat (context-events (:match new-route) (get-in db [::db :contexts]) new-contexts)
                             (page-events (get-in db [::db :route]) new-route))})

      {:db (-> db
               (assoc :fragment fragment)
               (assoc-in [::db :route] nil)
               (assoc-in [::db :not-found?] true))})))


(defn init
  [routes global-contexts]
  (swap! app-db
         #(-> %
              (assoc-in [::db :routes] routes)
              (assoc-in [::db :global-contexts] global-contexts)))
  #?(:cljs
     (do
       (zrf/dispatch [search-changed (.. js/window -location -search)])
       (aset js/window "onpopstate" #(zrf/dispatch [search-changed (.. js/window -location -search)]))
       (zrf/dispatch-sync [fragment-changed (.. js/window -location -hash)])
       (aset js/window "onhashchange" #(zrf/dispatch [fragment-changed (.. js/window -location -hash)])))))


(zrf/defe ::open-new-tab
  [{:keys [uri params]}]
  #?(:cljs
     (js/window.open
       (str uri (unparse-query-string params))
       "_blank")))


(zrf/defe ::page-redirect
  [{:keys [uri params]}]
  #?(:cljs
     (js/setTimeout
       #(set! (.-href (.-location js/window))
              (str uri (unparse-query-string params)))
       0)))


(zrf/defe ::redirect
  [{:keys [uri params]}]
  #?(:cljs
     (js/setTimeout
       #(set! (.-hash (.-location js/window))
              (str uri (unparse-query-string params)))
       0)))


(zrf/defe ::replace
  [{:keys [uri params]}]
  #?(:cljs
     (js/setTimeout
       #(.replaceState (.-history js/window)
                       nil nil
                       (str uri (unparse-query-string params)))
       0)))


(zrf/defe ::merge-params
  [params]
  (let [{:keys [fragment-path fragment-params]} @app-db]
    (zrf/dispatch [::redirect {:uri fragment-path
                               :params (merge (:params fragment-params) params)}])))


(zrf/defe ::merge-global-params
  [params]
  (let [{:keys [fragment global-params]} @app-db]
    #?(:cljs
       (js/setTimeout
         #(do (.pushState (.-history js/window)
                          nil nil
                          (str (unparse-query-string (merge global-params params)) fragment))
              (zrf/dispatch [search-changed (.. js/window -location -search)]))
         0))))


(zrf/defe ::back
  [_]
  #?(:cljs
     (.back js/history)))


(zrf/defs route
  [db _]
  (get-in db [::db :route]))


(zrf/defs not-found?
  [db _]
  (get-in db [::db :not-found?]))
