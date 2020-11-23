(ns app.users.core
  (:require
    [zframes.re-frame :as zrf]
    [anti.button]
    [app.routes :refer [href]]
    [stylo.core :refer [c]]
    [app.pages :as pages]
    [app.users.form]))

(zrf/reg-sub
  db
  (fn [db _] (get db ::db)))

(zrf/reg-event-fx
  load-users
  [(zrf/path ::db)]
  (fn [_ _]
    {:http/fetch [{:uri      "/User"
                   :unbundle true
                   :params   {:_count 1000}
                   :path     [::db :users]}]}))

(zrf/reg-sub
  users
  :<- [db]
  (fn [db _]
    (get-in db [:users :data])))


(zrf/reg-event-fx
  index
  [(zrf/path ::db)]
  (fn [_ [_ phase _]]
    (cond
      (= :init phase)
      {:dispatch-n [[load-users]]})))

(zrf/defview grid
  [users]
  [:div {:class (c :divide-y)}
   (for [u users]
     [:div {:key (:id u)
            :class (c :flex :flex-col [:py 1])}
      [:a {:class (c :font-bold)
           :href (href "users" (:id u))} (:id u)]
      [:div {:class (c [:space-x 2])}
       [:span {:class (c [:text :gray-600] :font-thin)} "email:"]
       (if (:email u) [:span {} (:email u)] [:span {:class (c :italic)} "not defined"])
       [:span {:class (c [:text :gray-600] :font-thin)} "role:"]
       (if (:roleName u) [:span {} (:roleName u)] [:span {:class (c :italic)} "not defined"])]])])

(zrf/defview page
  []
  [:div
   [:div {:class (c [:py 8] :w-max-full [:w 200] :mx-auto [:space-y 2])}
    [:div {:class (c :flex)}
     [anti.button/zf-button {:class    (c :ml-auto)
                             :type     "primary"
                             :on-click [:zframes.routing/redirect {:uri (href "users/new")}]}
      "New user"]]
    [grid]]])

(pages/reg-page index page)
