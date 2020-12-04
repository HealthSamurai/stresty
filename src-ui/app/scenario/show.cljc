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
            [reagent.dom :as dom]
            [app.scenario.editor :refer [zf-editor]]))

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

(zrf/defx run-step
  [_ [_ step idx]]
  {:http/fetch {:uri (str "/run-step")
                :method "post"
                :format "edn"
                :body (str step)
                :path [::db :scenario :data :steps idx :results]}})

(defmethod render-step 'stresty/http-step [step index]
  (let [method (first (filter meths (keys step)))]
    
    [:div {:class (c :flex :flex-col)}
     "Request" 
     [:div {:class (c [:pl 2] :flex-row)}
      method " " (get step method)
      [zf-button {:on-click [::run-step step index]} "Run"]]
     (if-let [body (:body step)]
       [:div
        [zf-editor [::db :scenario :data :steps index :body]]])
     (if-let [resp (get-in step [:results :data :response])]
       [:div
        "Response"
        [:div {:class (c [:pl 2])}
         [zf-editor [::db :scenario :data :steps index :results :data :response]]
         ]])]))

(defmethod render-step 'stresty.aidbox/truncate-step [step]
  [:div {:class (c :flex :flex-col)}
   [:div {:class (c :flex :flex-row)}
    "TRUNCATE " (str (:truncate step))
    [zf-button {:on-click [::run-step step index]} "Run"]]
   (if-let [resp (get-in step [:results :data :response])]
     [:div {:class (c [:pl 2])}
      [zf-editor [::db :scenario :data :steps index :results :data :response]]
      ]
     )
   ])

(defmethod render-step :default [step]
  [:pre (str step)])

(zrf/defview view [scenario]
  [:div {:class (c [:p 6])}
   [:h1 {:class (c :text-2xl [:mb 2])} (:title scenario)]
   [:div {:class (c [:mb 6])} (:desc scenario)]

   (for [[idx step] (map-indexed #(vector %1 %2) (:steps scenario))]
     ^{:key idx}
     [render-step step idx])

   ])

(pages/reg-page index view)
