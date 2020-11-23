(ns app.dashboard
  (:require [app.pages :as pages]
            [zframes.re-frame :as zrf]
            [stylo.core :refer [c]]
            [app.routes :refer [href]]))

(zrf/defx ctx
  [{db :db} [_ phase {params :params}]]
  (cond
    (= :deinit phase) {}

    (or (= :init phase) (= :params phase))
    {:http/fetch [{:uri "/dashboard"
                   :path [::model]}]}))

(zrf/defs model [db _]
  (get-in db [::model]))

(zrf/defview page [model]
  [:div {:class (c [:p 8])}
   "Dashboard"
   [:pre (pr-str model)]])

(pages/reg-page ctx page)
