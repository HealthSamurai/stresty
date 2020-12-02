(ns app.scenario.show
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
    {:http/fetch {:uri (str "/zen/symbol/" (:ns params) "/" (:name params))
                  :path [::db :scenario]}}
    :else
    {}))

(zrf/defs scenario [db]
  (get-in db [::db :scenario :data]))



(defmulti render-step (fn [step] (:type step)))



(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})

(defmethod render-step 'stresty/http-step [step]
  (let [method (first (filter meths (keys step)))]
    [:div
     [:div
      "Request"
      [:div {:class (c [:pl 2])}
       [:div method " " (get step method)]
       (if-let [body (:body step)]
         [:pre "Body: " (str body)])]]
     [:div
      "Response"
      [:div {:class (c [:pl 2])}
       [:div "Status: " (get-in step [:match :status] "any")]
       [:div "Body: " (str (get-in step [:match :body]))]]]]))

(defmethod render-step 'stresty.aidbox/truncate-step [step]
  [:div "TRUNCATE " (str (:truncate step))])

(defmethod render-step :default [step]
  [:pre (str step)])



(zrf/defview view [scenario]
  [:div {:class (c [:p 6])}
   [:h1 {:class (c :text-2xl [:mb 2])} (:title scenario)]
   [:div {:class (c [:mb 6])} (:desc scenario)]

   (for [[idx step] (map-indexed #(vector %1 %2) (:steps scenario))]
     ^{:key idx}
     [:div {:class (c [:mb 4] :flex)}
      (render-step step)])

   ])

(pages/reg-page index view)
