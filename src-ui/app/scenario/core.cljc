(ns app.scenario.core
  (:require [re-frame.core :as rf]
            [app.routes :refer [href]]
            [stylo.core :refer [c]]
            [app.pages :as pages]
            [zframes.re-frame :as zrf]
            [anti.select :refer [zf-select]]
            [anti.button :refer [zf-button]]
            [anti.input :refer [input]]
            [anti.util :refer [block]]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(zrf/defx index
  [{db :db} [_ phase params]]
  (cond

    (= :init phase)
    {:http/fetch {:uri "/scenarios"
                  :path [::db :scenarios]}}

    :else
    {}))

(zrf/defs scenarios [db]
  (get-in db [::db :scenarios :data]))

(zrf/defview view [scenarios]
  [:div {:class (c [:p 6])}
   [:h1 {:class (c :text-2xl [:mb 2])} "Scenarios"]
   (for [scenario scenarios]
     ^{:key (:zen/name scenario)}
     [:a {:class (c [:mb 2])
          :href (href "scenario" (namespace (:zen/name scenario)) (name (:zen/name scenario)))}
      [:div {:class (c :text-lg)} (:title scenario)]
      [:div (:desc scenario)]])])

(pages/reg-page index view)
