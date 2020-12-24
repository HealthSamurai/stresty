(ns app.hack.core
  (:require [app.pages :as pages]
            [stylo.core :refer [c]]
            [re-frame.core :as rf]
            [zframes.re-frame :as zrf]
            [app.scenario.editor]
            ))


(zrf/defx ctx
  [{db :db} [_ phase params]]
  (cond
    (= :init phase)
    {:db (-> db
             (assoc-in [::db :id] (:id params))
             (assoc-in [::db :config] {:url "https://little.aidbox.app"}))}))

(zrf/defx update-config [{db :db} [_ field value]]
  {:db (assoc-in db [::db :config field] value)})

(def setup-data
  {:resourceType "Bundle"
   :type "trasaction"
   :entry [{:request {:url "/Entity/StrestyCase" :method "PUT"}
            :resource {:type "resource"
                       :resourceType "Entity"
                       :isOpen true}}
           {:request {:url "/Entity/StrestyStep" :method "PUT"}
            :resource {:type "resource"
                       :resourceType "Entity"
                       :isOpen true}}]})

(zrf/defx setup-aidbox [{db :db} _]
  (let [config (get-in db [::db :config])]
    {:http/fetch {:uri (str (:url config) "/")
                  :method "post"
                  :format "json"
                  :headers {:content-type "application/json"}
                  :body setup-data
                  :path [::db :config-resp]}}))

(zrf/defx get-steps-success [{db :db} [_ {data :data :as resp}]]
  {:db (assoc-in db [::db :steps] (reduce (fn [steps step] (assoc steps (:id step) step)) {} data))})

(zrf/defx get-steps [{db :db} _]
  {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyStep")
                :params {:.case.id (get-in db [::db :id])}
                :format "json"
                :unbundle true
                :headers {:content-type "application/json"}
                :success {:event get-steps-success}}})

(zrf/defx create-case [{db :db} _]
  {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyCase/" (get-in db [::db :id]))
                :method "put"
                :format "json"
                :headers {:content-type "application/json"}
                :success {:event get-steps}
                :body {:type "tutorial" :steps []}
                :path [::db :case]}})

(zrf/defx get-or-create-case [{db :db} _]
  {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyCase/" (get-in db [::db :id]))
                :format "json"
                :headers {:content-type "application/json"}
                :error {:event create-case}
                :success {:event get-steps}
                :path [::db :case]}})



(zrf/defs page-sub [db] (get db ::db))

(zrf/defs config [db] (get-in db [::db :config]))

(zrf/defview config-view [config]
  [:div
    (let [input-cls (c [:border])]
      [:div {:class (c [:p 2])}
       [:div
        [:span "Aidbox URL"]
        [:input {:class [input-cls] :value (:url config) :on-change #(rf/dispatch [update-config :url (.-value (.-target %))])}]]
       [:div
        [:span "Auth header"]
        [:input {:class [input-cls] :value (:auth config) :on-change #(rf/dispatch [update-config :auth (.-value (.-target %))])}]]
       [:input {:type "button" :value "Submit" :on-click #(rf/dispatch [setup-aidbox])}]
       [:input {:class (c [:ml 2]) :type "button" :value "Init" :on-click #(rf/dispatch [get-or-create-case])}]
       ])])



(zrf/defx create-step-success [{db :db} [_ {idx :idx step :data}]]
  (let [case (update (get-in db [::db :case :data]) :steps conj {:id (:id step) :resourceType "StrestyStep"})]
    {:db (-> db
             (assoc-in [::db :steps (:id step)] step)
             (assoc-in [::db :case :data] case))
     :http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyCase/" (get-in db [::db :id]))
                  :format "json"
                  :method "put"
                  :body case}}))

(zrf/defx create-step [{db :db} [_ idx]]
  {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyStep")
                :method "post"
                :format "json"
                :headers {:content-type "application/json"}
                :body {:case {:id (get-in db [::db :id]) :resourceType "StrestyCase"}
                       :type "sql"
                       :sql ""}
                :success {:event create-step-success
                          :idx idx}}})


(zrf/defs steps [db]
  (get-in db [::db :steps]))

(zrf/defs stresty-case [db]
  (get-in db [::db :case :data]))

(zrf/defx update-step-value [{db :db} [_ step-id field value]]
  {:db (assoc-in db [::db :steps step-id field] value)})


(defn get-http-fetch-for-step [config step]

  (cond
    (= "sql" (:type step))
    {:uri (str (:url config) "/$sql")
     :method "post"
     :format "json"
     :body [(:sql step)]}

    :else
    (throw (ex-info "no such step type" {}))))

(zrf/defx on-exec-step [{db :db} [_ {status ::status step-id :step-id data :data}]]
  (let [step (-> (get-in db [::db :steps step-id])
                 (assoc-in [:status] status)
                 (assoc-in [:result] data))]
    {:db (assoc-in db [::db :steps step-id] step)
     :http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyStep/" step-id)
                  :method "put"
                  :format "json"
                  :body (dissoc step :result)}}))


(zrf/defx exec-step [{db :db} [_ step-id]]
  (println "exec" (get-in db [::db :steps step-id]))
  (let [step (get-in db [::db :steps step-id])
        http-fetch (get-http-fetch-for-step (get-in db [::db :config]) step)
        on-complete {:event on-exec-step
                     :step-id step-id}]
    {:http/fetch (merge http-fetch
                        {:success (assoc on-complete ::status "ok")
                         :error (assoc on-complete ::status "error")})}))

(defn render-step [step]
  [:div
   (str step)
   [:div (:id step)]
   [:div (:sql step)]
   [app.scenario.editor/zf-editor
    {:opts {"extraKeys" {"Ctrl-Enter" #(rf/dispatch [exec-step (:id step)])}}
     :on-change #(rf/dispatch [update-step-value (:id step) :sql %])
     :value (:sql step)}]])


(zrf/defview view [stresty-case steps]
  [:<>
   [config-view]

   [:div {:class (c [:p 2])}
    [:input {:type "button" :value "Add" :on-click #(rf/dispatch [create-step :last])}]]

   (for [[idx step-id] (map-indexed (fn [idx step] [idx (:id step)]) (:steps stresty-case))]
     ^{:key step-id}
     (if-let [step (get steps step-id)]
       [render-step step]
       [:div "loading..."]))])



(pages/reg-page ctx view)
