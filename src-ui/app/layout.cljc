(ns app.layout
  (:require [zframes.re-frame :as zrf]
            [stylo.core :refer [c]]
            [clojure.string :as str]))

(zrf/defs current-uri [db]
 (:uri (first (:route/history db))))

(zrf/defs cases
  [db _]
  (:cases db))

(zrf/defx select-case
  [{db :db} [_ case-id]]
  {:db (-> db
           (assoc :current-case case-id))
   :dispatch [:zframes.routing/page-redirect {:uri "#/case"}]}
  )

(zrf/defview case-list [cases]
  [:div {:class (c :flex :flex-col [:pl 4] [:pr 4])}
   [:a {:class (c [:text :white] :text-base [:p 2])
        :on-click #(zrf/dispatch [:zframes.routing/page-redirect {:uri "#/scenario"}])}
    "Scenarios"
    ]
   [:a {:class (c [:text :white] :text-base [:p 2])
        :on-click #(zrf/dispatch [:zframes.routing/page-redirect {:uri "#/config"}])}
    "Config"
    ]
   #_(for [case (vals cases)]
     ^{:key (:id case)}
      [:a {:class (c [:text :white] :text-base [:p 2])
              :on-click #(zrf/dispatch [::select-case (get-in case [:id])])}
          [:span (str (:desc case))]]
     )
   ]
  )

(zrf/defview quick-menu []
  [:<>
   [:div {:class (c [:z 100] :overflow-hidden :flex :flex-col
                    {:background-color "#9CA3AF"
                     :box-shadow       "4px 0 6px -1px rgba(0, 0, 0, 0.1), 2px 0 4px -1px rgba(0, 0, 0, 0.09)"})}
    [case-list]
    [:div {:class (c :flex-1)}]
    ]])


(defn layout [content]
  [:div {:class (c :flex :items-stretch :h-screen)}
   [:style "body {padding: 0; margin: 0;}"]
   [quick-menu]
   [:div {:class (c :flex-1 :overflow-y-auto)} content]])
