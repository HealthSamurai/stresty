(ns app.core
  (:require #?(:cljs [reagent.dom])
            [re-frame.core :as rf]
            [zframes.re-frame :as zrf]
            [zframes.cookies]
            [zframes.routing :refer [route not-found?]]
            [zframes.debounce]
            [zframes.window]
            [zframes.console]
            [zframes.storage]
            #?(:cljs [zframes.http])
            [zframes.hotkeys]
            [zf.core]
            [app.routes :as routes]
            [app.layout]
            [app.pages :as pages]

            [app.anti]
            [app.dashboard]
            [app.case.core]
            [app.scenario.core]
            [app.scenario.show]
            #?(:cljs [app.reagent])
            [app.reframe]))

(def cases
  [{:id :simple-crud
    :desc "Simple CRUD Tutorial"
    :steps [{:id :create-patient
              :desc "You should create patient to start use Jedi force"
              :POST "/Patient"
              :body {:id "new-patient"
                     :name [{:given ["Luke"]
                             :family "Skywalker"}]
                     :birthDate "2145-08-12"
                     :gender "male"}}
            {:id :patch-user
             :desc "Now need to update user to link it to our new patient"
             :PATCH "/Users/postman"
             :body {:data {:patient_id "new-patient"}}}
            {:id :match-user
             :desc "Check that user has acess to patient"
             :GET "Patient/new-patient"}]}
   {:id :access-patient-data
    :desc "Access Patient Data"
    :steps [{:id :create-patient
             :desc "Create patient"
             :POST "/Patient"
             :body {:id "/obi-wan"
                    :name [{:given ["Obi-Wan"]
                            :family "Kenobe"}]
                    :birthDate "2126-12-12"
                    :gender "male"}}
            {:id :make-obi-wan-jedi
             :desc "Make Obi-Wan great Jedi again"
             :PATCH "/Patient/obi-wan"
             :body {:gender "Jedi"}}
            ]}
   ])

(zrf/defview current-page
  [route not-found?]
  [app.layout/layout
   (if not-found?
     [:div.not-found (str "Route not found")]
     (if-let [page (get @pages/pages (:match route))]
       [page (:params route)]
       [:div.not-found (str "Page not defined [" (:match route) "]")]))])

(rf/reg-event-fx
 ::initialize
 (fn [{db :db} _]
   {:db (assoc db :cases (into {} (map (juxt :id identity) cases)))
    :dispatch [:zframes.routing/page-redirect {:uri "#/scenario"}]}))

(defn mount-root []
  (rf/clear-subscription-cache!)
  #?(:cljs
     (reagent.dom/render
      [current-page]
      (.getElementById js/document "app"))))

(defn init! []
  (zframes.routing/init routes/routes routes/global-contexts)
  (rf/dispatch [::initialize])
  (mount-root))
