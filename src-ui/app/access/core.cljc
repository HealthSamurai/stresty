(ns app.access.core
  (:require
   [re-frame.core :as rf]
   [app.routes :refer [href]]
   [stylo.core :refer [c]]
   [app.pages :as pages]
   [anti.button]))

;; Test URI
;; -------------

(defonce pt-uri "/Patient")
(defonce enc-uri "/Encounter")
(defonce observ-uri "/Observation")

(rf/reg-sub
 ::page-data
 (fn [db [_]] (get db ::db)))

(rf/reg-sub
 ::user-info
 (fn [db [_]] (get-in db [:userinfo :data])))

(rf/reg-event-fx
 ::index
 (fn [{db :db} [_ phase _params]]
   (cond
     (= :init phase)
     {:http/fetch [{:uri      "/User"
                    :unbundle true
                    :params   {:_count 1000}
                    :path     [::db :users-list]}]}

     (= :deinit phase)
     {:db (dissoc db ::db)}

     :else
     {})))

(rf/reg-event-fx
 ::featch-data
 (fn [{db :db} [_ uri]]
   (let [token (get-in db [::db :auth-user :data :access_token])]
     {:db (assoc-in db [::db :uri] uri)
      :http/fetch [{:uri uri
                    :headers {"authorization" (str "Bearer " token)}
                    :path [::db :response]}]})))

(rf/reg-event-fx
 ::delete-data
 (fn [{db :db} [_ uri]]
   (let [token (get-in db [::db :auth-user :data :access_token])]
     {:db (assoc-in db [::db :uri] uri)
      :http/fetch [{:uri uri
                    :method :delete
                    :headers {"authorization" (str "Bearer " token)}
                    :path [::db :response]}]})))

(rf/reg-event-fx
 ::get-auth-user
 (fn [_ [_ user-id]]
   {:http/fetch [{:uri   "/auth/token"
                  :method "post"
                  :body {:grant_type "password"
                         :client_id "myapp"
                         :client_secret "verysecret"
                         :username user-id
                         :password "admin"}
                  :path [::db :auth-user]}]}))

(rf/reg-event-fx
 ::on-select-user
 (fn [{db :db} [_ user-id]]
   (let [users-list (get-in db [::db :users-list :data])
         user (first (filter #(= user-id (:id %)) users-list))]
     {:db (-> db
              (assoc-in [::db :selected-user] user)
              (assoc-in [::db :uri] nil)
              (assoc-in [::db :response] nil))
      :dispatch [::get-auth-user user-id]})))

(rf/reg-event-fx
 ::on-change-observation
 (fn [{db :db} [_ value]]
   {:db (assoc-in db [::db :observation-data] value)}
   )
 )

(rf/reg-event-fx
 ::create-observation
 (fn [{db :db} [_ _]]
   (let [observation-data (get-in db [::db :observation-data])
         token (get-in db [::db :auth-user :data :access_token])]
     {:http/fetch [{:uri "/Observation"
                    :method "post"
                    :body observation-data
                    :headers {"authorization" (str "Bearer " token)}
                    :path [::db :response]}]})))

(rf/reg-event-fx
 ::patch-observation
 (fn [{db :db} [_ _]]
   (let [observation-data (get-in db [::db :observation-data])
         token (get-in db [::db :auth-user :data :access_token])]
     {:http/fetch [{:uri "/Observation/observation-3"
                    :method "patch"
                    :body observation-data
                    :headers {"authorization" (str "Bearer " token)}
                    :path [::db :response]}]})))

(rf/reg-event-fx
 ::put-observation
 (fn [{db :db} [_ _]]
   (let [observation-data (get-in db [::db :observation-data])
         token (get-in db [::db :auth-user :data :access_token])]
     {:http/fetch [{:uri "/Observation/observation-3"
                    :method "put"
                    :body observation-data
                    :headers {"authorization" (str "Bearer " token)}
                    :path [::db :response]}]})))

;; styles
;; ---------------


(def fieldset (c :w-auto {:border "1px solid #eee"} [:p 1]))

(def legend (c :w-auto :text-sm [:mb 0]))

(def select (c :cursor-pointer [:w "100%"] [:bg :white] [:focus {:outline "none"}]))


;; template
;; ---------------


(defn view [_params]
  (let [page-data @(rf/subscribe [::page-data])
        token (get-in page-data [:auth-user :data :access_token])
        users (get-in page-data [:users-list :data])
        uri (get-in page-data [:uri])
        response-data (or (get-in page-data [:response :data]) (get-in page-data [:response :error]))
        selected-user (:selected-user page-data)
        user-id (or (:id selected-user) "")
        email (or (:id selected-user) "")
        roleName (or (:id selected-user) "")
        resourceType (or (:id selected-user) "")
        patient-id (get-in selected-user [:data :patient_id])]

    [:div {:class (c :w-max-full [:w 200] :mx-auto)}
     [:div {:class (c [:py 8] [:space-y 2])}

      [:h2 {:class (c :text-base)}
       "Case 1: get patient data"]

      [:fieldset {:class fieldset}
       [:legend {:class legend}
        [:a {:href (href "users" user-id)} "User"]]

       [:select {:class select
                 :name       "user_id"
                 :value      user-id
                 :on-change  #(rf/dispatch [::on-select-user (-> % .-target .-value)])}
        [:option {:value "" :disabled true} ""]
        (for [u users]
          [:option {:key (:id u) :value (:id u)} (:id u)])]]

      (when (not= user-id "")
        [:<>
         [:div {:class (c :flex [:space-x 2])}

          [:div {:class (c :flex [:space-x 2])}
           [:span {:class (c [:text :gray-600] :font-thin)} "email:"]

           (if email
             [:span {} email]
             [:span {:class (c :italic)} "not defined"])

           [:span {:class (c [:text :gray-600] :font-thin)} "role:"]
           (if roleName
             [:span {} roleName]
             [:span {:class (c :italic)} "not defined"])]

          [:span {:class (c [:text :gray-600] :font-thin)} "resourceType:"]
          (if resourceType
            [:span {} resourceType]
            [:span {:class (c :italic)} "not defined"])]
         [:div {:class (c {:background-color "#bbd2f5"} [:p "7px"])}
          [:div "URI: " uri]
          [:div "Token: " token]
          [:div (str "response: " response-data)]]

         [:div {:class (c :flex [:space-x 2])}
          [:button {:class anti.button/base-class
                    :on-click #(rf/dispatch [::featch-data (str pt-uri "/" patient-id)])}
           "Get patient"]

          [:button {:class anti.button/base-class
                    :on-click #(rf/dispatch [::featch-data (str enc-uri "?patient=" patient-id)])}
           "Get encounter"]

          [:button {:class anti.button/base-class
                    :on-click #(rf/dispatch [::featch-data (str observ-uri "?patient=" patient-id)])}
           "Get observation"]

          ]

         [:div {:class (c :flex [:space-x 2])}
          [:button {:class anti.button/base-class
                    :on-click #(rf/dispatch [::delete-data (str observ-uri "/observation-3")])}
           "Can delete observation"]

          [:button {:class anti.button/base-class
                    :on-click #(rf/dispatch [::delete-data (str observ-uri "/observation-2")])}
           "Cannot delete observation"]]

         [:div {:class (c :flex {:flex-wrap "wrap" })}
          
          [:textarea {:class (c {:width "100%" :margin-bottom "5px" :min-height "400px" :border "1px solid #eee"})
                      :on-change #(rf/dispatch [::on-change-observation (-> % .-target .-value)])
                      :value (get-in page-data [:observation-data])}]
          
          [:button {:class anti.button/base-class
                    :on-click #(rf/dispatch [::create-observation])
                    }
           "create observation"]

          [:button {:class anti.button/base-class
                    :on-click #(rf/dispatch [::patch-observation])
                    }
           "patch observation"]

          [:button {:class anti.button/base-class
                    :on-click #(rf/dispatch [::put-observation])
                    }
           "put observation"]
          
          ]
         
         ])]]))
 

(pages/reg-page ::index #'view)
