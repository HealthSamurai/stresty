(ns app.config.core
  (:require [re-frame.core :as rf]
            [zframes.re-frame :as zrf]
            [app.scenario.editor :refer [zf-editor]]
            [stylo.core :refer [c]]
            [app.pages :as pages])
  )

(zrf/defx index
  [{db :db} _]  )



(defn view []
  [:div {:class (c :flex :flex-col [:p 2])}
   [:h1 {:class (c :text-2xl [:mb 2])} "Config"]
   [zf-editor [:config]]])

(pages/reg-page index view)

