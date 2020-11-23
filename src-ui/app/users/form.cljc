(ns app.users.form
  (:require
    [zframes.re-frame :as zrf]
    [anti.file]
    [anti.input]
    [anti.button]
    [anti.radio]
    [anti.native-select]
    [app.routes :refer [href]]
    [stylo.core :refer [c]]
    [app.pages :as pages]
    [clojure.string :as str]
    [zf.core :as zf]
    [re-frame.core :as rf]))

(zrf/reg-sub
  db
  (fn [db _] (get db ::db)))

(zrf/reg-event-fx
  load-user-ok
  [(zrf/path ::db)]
  (fn [{:keys [db]} [_ {data :data}]]
    {:db (zf/set-value db {:zf/root [:form]}
                       (dissoc data :meta :resourceType :password))}))

(zrf/reg-event-fx
  load-user
  (fn [{:keys [db]} [_ user-id]]
    {:http/fetch [{:uri      (str "/User/" user-id)
                   :unbundle true
                   :path     [::db :user]
                   :success  {:event load-user-ok}}]}))

(zrf/reg-event-fx
  submit-ok
  [(zrf/path ::db)]
  (fn [_ [_ {:keys [data]}]]
    {:zframes.routing/redirect {:uri (href "users")}}))

(zrf/reg-event-fx
  upload-avatar
  [(zrf/path ::db)]
  (fn [{:keys [db]} [_ { {url :url} :data}]]
    (let [#_#_avatar (. js/user-avatar -img)
          avatar (:user-avatar db)]
      {:http/fetch {:uri     url
                    :method "put"
                    :format "bin"
                    :success {:event submit-ok}
                    :files avatar}})))

(zrf/reg-event-fx
  get-upload-url
  [(zrf/path ::db)]
  (fn [_ [_ {:keys [data]}]]
    {:http/fetch {:uri     (str "/User/" (:id data) "/$url")
                  :success {:event upload-avatar}}}))

(zrf/reg-event-fx
  submit-error
  [(zrf/path ::db)]
  (fn [_ _]
    {:app/notify {:type    "error"
                  :message "Unable to save changes. Make sure you filled all required fields."}}))


(zrf/reg-sub
  user
  :<- [db]
  (fn [db _] (get-in db [:user :data])))


(zrf/reg-sub
 user-loading
 :<- [db]
 (fn [db _] (get-in db [:user :loading])))


(zrf/reg-event-fx
  submit
  [(zrf/path ::db)]
  (fn [{:keys [db]} _]
    (let [user (get-in db [:user :data])
          data (zf/value db {:zf/root [:form]})]
      {:http/fetch {:uri     (cond-> "/User" (:id user) (str "/" (:id user)))
                    :method  (if (:id user) "patch" "post")
                    :body    (cond-> data
                               (str/blank? (:password data)) (dissoc :password))
                    :success {:event (if (:user-avatar db)
                                       get-upload-url
                                       submit-ok)}
                    :error   {:event submit-error}}})))

(zrf/reg-event-fx
  edit-password
  [(zrf/path ::db)]
  (fn [{:keys [db]} _]
    {:db (assoc db :password-editable? true)
     :dispatch [:dom/focus (zf/get-id {:zf/root [::db :form] :zf/path [:password]})]}))

(zrf/reg-event-fx
  index
  [(zrf/path ::db)]
  (fn [{:keys [db]} [_ phase {id :user-id}]]
    (cond
      (= :deinit phase)
      {:db nil}

      (or (= :params phase) (= :init phase))
      {:dispatch-n [(when id [load-user id])]
       :db         (-> db
                       (assoc-in [:form :value :roleName] "biller")
                       (assoc :password-editable? (nil? id)))})))


(zrf/reg-sub
  password-editable?
  :<- [db]
  (fn [db _]
    (get db :password-editable?)))


(zrf/reg-event-fx
  save-image
  [(zrf/path ::db)]
  (fn [{:keys [db]} [_ image]]
    {:db (assoc db :user-avatar image)}))

(defn save-avatar [avatar]
  (rf/dispatch [save-image avatar]))


(zrf/defview form
  [user user-loading password-editable?]
  [:div {:class (c :flex [:space-x 6])}
   (when-not user-loading
     [anti.file/photo {:on-change save-avatar :value (when-let [id (:id user)]
                                                       (str "/User/" id "/$image" "?" (rand-int 1000)))}])
   [:div {:class (c [:space-y 8] :flex-1)}

    [:div {:class (c [:space-y 4])}
     [:div {:class (c :flex :justify-between :border-b :items-center [:pb 1] [:h 8] [:box-content])}
      [:h3 {:class (c [:text-lg] [:text :gray-600])} "Personal data"]]
     [:div {:class (c :grid [:col-gap 4] [:row-gap 3] [:items-center] {:grid-template-columns "5rem 1fr 5rem 1fr"})}
      [:label "First name"]
      [anti.input/zf-input {:opts {:zf/root [::db :form] :zf/path [:name :givenName]} :auto-complete "off"}]
      [:label "Last name"]
      [anti.input/zf-input {:opts {:zf/root [::db :form] :zf/path [:name :familyName]} :auto-complete "off"}]
      [:label "Username" [:span {:class (c [:text :red-500] [:ml 0.5])} "*"]]
      [anti.input/zf-input {:disabled (some? user)
                            :opts {:zf/root [::db :form] :zf/path [:id]} :auto-complete "off"}]
      [:label "Email"]
      [anti.input/zf-input {:opts {:zf/root [::db :form] :zf/path [:email]} :auto-complete "off" :type "email"}]
      [:label "Password" [:span {:class (c [:text :red-500] [:ml 0.5])} "*"]]
      (if password-editable?
        [anti.input/zf-input {:opts {:zf/root [::db :form] :zf/path [:password]} :auto-complete "off" :type "password"}]
        [:div {:class (c :flex [:space-x 2])}
         [anti.input/input {:type "password" :value "password" :disabled true}]
         [anti.button/zf-button {:on-click [edit-password]} "Edit"]])
      [:label "Role" [:span {:class (c [:text :red-500] [:ml 0.5])} "*"]]
      [anti.native-select/zf-select {:opts {:zf/root [::db :form] :zf/path [:roleName]}
                                     :auto-complete "off"
                                     :value "biller"}
       [:option {:value "biller"} "Biller"]
       [:option {:value "admin"} "Admin"]
       [:option {:value "import-scans"} "Import Scans"]]]]

    [:div {:class (c :flex)}
     [:div {:class (c :ml-auto [:space-x 2])}
      [anti.button/zf-button {:on-click [:zframes.routing/redirect {:uri (href "users")}]}
       "Cancel"]
      [anti.button/zf-button {:on-click [submit] :type "primary"}
       "Save"]]]]])


(zrf/defview fake-form
  []
  [:div {:style {:display "none"}}
   [:input {:type "text" :name "email"}]
   [:input {:type "password" :name "password" :auto-complete "new-password"}]])


(zrf/defview page
  []
  [:div
   [:div {:class (c [:py 8] :w-max-full [:w 220] :mx-auto)}
    [fake-form]
    [form]]])

(pages/reg-page index page)
