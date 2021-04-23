(ns app.layout
  (:require [zframes.re-frame :as zrf]
            [stylo.core :refer [c]]
            [clojure.string :as str]
            [app.routes :refer [href]]
            ))

(zrf/defs current-uri [db]
 (:uri (first (:route/history db))))

(zrf/defs namespaces
  [db _]
  (get-in db [:app.scenario.core/db :namespaces :data :namespaces]))

(zrf/defview case-list [namespaces]
  [:div {:class (c :flex :flex-col [:p 4] [:h "100%"] :justify-between [:w-min 50])}
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
        ])])
  
   [:div 
    [:a {:class (c :text-base [:p 2])
         :on-click #(zrf/dispatch [:zframes.routing/page-redirect {:uri "#/config"}])}
     [:i.fas.fa-cog]
     " Config"
     ]]
   
   ]
  )

(zrf/defview quick-menu []
  [:<>
   [:div {:class (c [:z 100] :overflow-hidden :flex :flex-col
                    {:background-color (c [:bg :gray-200])})}
    [case-list]
    [:div {:class (c :flex-1)}]
    ]])


(defn layout [content]
  [:div {:class (c :flex :items-stretch :h-screen)}
   [:style "body {padding: 0; margin: 0;}"]
   [quick-menu]
   [:div {:class (c :flex-1 :overflow-y-auto)}
    content]])
