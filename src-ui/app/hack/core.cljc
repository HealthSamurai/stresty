(ns app.hack.core
  (:require [app.pages :as pages]
                        [stylo.core :refer [c]]

 [re-frame.core :as rf]           [zframes.re-frame :as zrf]
))


(zrf/defx ctx
  [{db :db} [_ phase params]]
  )

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
        [:input {:class [input-cls] :value (:auth config) :on-change #(rf/dispatch [update-config :auth (.-value (.-target %))])}]
        ]
       [:input {:type "button" :value "Submit" :on-click #(rf/dispatch [setup-aidbox])}]])


    ])

(zrf/defview view []
  [:<>
   [config-view]



   [:div]

   ])


(pages/reg-page ctx view)
