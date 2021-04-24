(ns app.scenario.core
  (:require [app.routes :refer [href]]
            [stylo.core :refer [c]]
            [app.pages :as pages]
            [zframes.re-frame :as zrf]))

(zrf/defx index
  [{db :db} [_ phase params]]
  (cond
    
    (= :init phase)
    {:zen/rpc {:method "sty/get-namespaces"
               :path [::db :namespaces]}}

    :else
    {}))

(zrf/defs namespaces [db]
  (get-in db [::db :namespaces :data :namespaces]))

(zrf/defview view [namespaces]
  (let [items (keys namespaces)]
    [:div {:class (c [:p 6])}
     [:h1 {:class (c :text-2xl [:mb 2])} "Scenarios"]
     (for [ns items]
       ^{:key ns}
       [:div {:class (c [:mb 2])}
        [:div {:class (c :text-lg :cursor-pointer)} ns]
        [:div {:class (c [:ml 2])}
         (for [case (get namespaces ns)]
           ^{:key case}
           [:div
            [:a {:href (href "scenario" case)}(str case)]]
           )
         ]
        ])]))

(pages/reg-page index view)
