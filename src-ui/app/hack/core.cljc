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

(zrf/defx get-steps [{db :db} _]
  {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyStep")
                :params {:.case.id (get-in db [::db :id])}
                :format "json"
                :unbundle true
                :headers {:content-type "application/json"}
                :path [::db :steps]}})

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




(zrf/defx create-step [{db :db} [_ idx]]
  (if (= :last idx)
    {:db (update-in db [::db :steps :data] conj {:type "sql"
                                                 :sql ""})}))


(zrf/defs steps [db]
  (get-in db [::db :steps :data]))


(zrf/defx update-step-value [{db :db} [_ step-idx field value]]
  (prn (type (get-in db [::db :steps :data])))
  {:db (assoc-in db [::db :steps :data step-idx field] value)})

(zrf/defview view [steps]
  [:<>
   [config-view]


   [:div {:class (c [:p 2])}
    [:input {:type "button" :value "Add" :on-click #(rf/dispatch [create-step :last])}]]

   (for [[idx step] (map-indexed (fn [idx step] [idx step])  steps)]
     [:div
      (:type step)
      [app.scenario.editor/zf-editor {:on-change #(rf/dispatch [update-step-value idx :sql %]) :value (:sql step)}]
      ])

   ])


(pages/reg-page ctx view)
