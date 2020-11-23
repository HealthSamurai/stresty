(ns app.layout
  (:require [zframes.re-frame :as zrf]
            [stylo.core :refer [c]]
            [clojure.string :as str]))

(zrf/defs current-uri [db]
 (:uri (first (:route/history db))))


(zrf/defview quick-menu []
  [:<>
   [:div {:class (c [:z 100] :overflow-hidden :flex :flex-col
                    {:background-color "#2c3645"
                     :box-shadow       "4px 0 6px -1px rgba(0, 0, 0, 0.15), 2px 0 4px -1px rgba(0, 0, 0, 0.09)"})}
    [:span "hahha"]
    [:div {:class (c :flex-1)}]
    ]])


(defn layout [content]
  [:div {:class (c :flex :items-stretch :h-screen)}
   [:style "body {padding: 0; margin: 0;}"]
   [quick-menu]
   [:div {:class (c :flex-1 :overflow-y-auto)} content]])
