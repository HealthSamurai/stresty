(ns app.core
  (:require #?(:cljs [reagent.dom])
            [re-frame.core :as rf]
            [zframes.re-frame :as zrf]
            [zframes.cookies]
            [zframes.routing :refer [route not-found?]]
            [zframes.debounce]
            [zframes.window]
            [zframes.console]
            [zframes.storage]
            #?(:cljs [zframes.http])
            [zframes.hotkeys]
            [zf.core]
            [app.routes :as routes]
            [app.layout]
            [app.pages :as pages]

            [app.anti]
            [app.dashboard]
            [app.users.core]
            [app.access.core]
            [app.patients.core]
            #?(:cljs [app.reagent])
            [app.reframe]))

(zrf/defview current-page
  [route not-found?]
  [app.layout/layout
   (if not-found?
     [:div.not-found (str "Route not found")]
     (if-let [page (get @pages/pages (:match route))]
       [page (:params route)]
       [:div.not-found (str "Page not defined [" (:match route) "]")]))])

(rf/reg-event-fx
 ::initialize
 (fn [_ _]
   {:zframes.routing/page-redirect {:uri "#/"}}))

(defn mount-root []
  (rf/clear-subscription-cache!)
  #?(:cljs
     (reagent.dom/render
      [current-page]
      (.getElementById js/document "app"))))

(defn init! []
  (zframes.routing/init routes/routes routes/global-contexts)
  (rf/dispatch [::initialize])
  (mount-root))
