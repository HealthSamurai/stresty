(ns app.core
  (:require [re-frame.core :as rf]
            [zframes.re-frame :as zrf]
            [zframes.cookies]
            [zframes.routing :refer [route not-found?]]
            [zframes.debounce]
            [zframes.window]
            [zframes.console]
            [zframes.storage]

            [app.pages :as pages]

            [zframes.hotkeys]
            [zf.core]
            [app.routes :as routes]

            [app.dashboard]
            [app.anti]
            [app.layout]

            [app.case.core]
            [app.scenario.core]
            [app.scenario.show]
            [app.config.core]

            #?(:cljs [zframes.http])
            #?(:cljs [app.reagent])
            #?(:cljs [reagent.dom])))

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
 (fn [{db :db} _]
   {:db (assoc db :config  {:url "http://access-policy-box.aidbox.io"
                           :agents {:default {:type 'stresty/basic-auth
                                              :client-id "stresty"
                                              :client-secret "stresty"}
                                    :client {:type 'stresty/auth-client}}})
    :dispatch [:zframes.routing/page-redirect {:uri "#/scenario"}]}))

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
