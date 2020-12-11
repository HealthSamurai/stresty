(ns app.layout
  (:require [zframes.re-frame :as zrf]
            [stylo.core :refer [c]]
            [clojure.string :as str]
            [app.routes :refer [href]]
            ))

(zrf/defs current-uri [db]
 (:uri (first (:route/history db))))

(zrf/defs scenarios
  [db _]
  (get-in db [:app.scenario.core/db :scenarios :data]))

(zrf/defview case-list [scenarios]
  [:div {:class (c :flex :flex-col [:p 4] [:h "100%"] :justify-between [:w-min 50])}
   [:div
    [:span {:class (c :text-2xl)} "Cases"]
    (for [scenario scenarios]
      ^{:key (:zen/name scenario)}
      [:a {:href (href "scenario" (namespace (:zen/name scenario)) (name (:zen/name scenario)))}
       [:div {:class (c :text-base :flex :flex-row :items-center [:py 1])}
        [:i.fas.fa-circle {:class (c {:font-size "8px" }[:pr 2])}]
        (:title scenario)
        ]
       ]
      )
    ]
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
                    {:background-color "#f7f6f3"
                     })}
    [case-list]
    [:div {:class (c :flex-1)}]
    ]])


(defn layout [content]
  [:div {:class (c :flex :items-stretch :h-screen)}
   [:style "body {padding: 0; margin: 0;}"]
   [quick-menu]
   [:div {:class (c :flex-1 :overflow-y-auto)} content]])
