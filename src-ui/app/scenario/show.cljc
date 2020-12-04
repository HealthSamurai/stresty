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
    (let [config (get db :config)
          config* (assoc config :current-case (str (:ns params) "/" (:name params)))]
      {:http/fetch [{:uri (str "/zen/symbol/" (:ns params) "/" (:name params))
                     :path [::db :scenario]}
                    {:uri "/create-new-ctx"
                     :method "POST"
                     :headers {:content-type "application/edn"}
                     :body  (str config*)
                     :path [::db :ctx]}]})
    :else
    {}))

(zrf/defx create-ctx [{db :db} _]
  (let [config (get db :config)
        config* (assoc config :current-case (get-in db [::db :scenario :data :zen/name]))]
    {:http/fetch
     {:uri "/create-new-ctx"
      :method "POST"
      :headers {:content-type "application/edn"}
      :body (str config*)
      :path [::db :ctx]}}))

(zrf/defs scenario [db]
  (get-in db [::db :scenario :data]))

(zrf/defs case-ctx [db]
  (get-in db [::db :ctx :data]))

(defmulti render-step (fn [step] (:type step)))

(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})

(zrf/defx run-step
  [{db :db} [_ step idx]]
  (let [case-ctx (get-in db [::db :ctx :data])]
    {:http/fetch {:uri (str "/run-step")
                  :method "post"
                  :format "edn"
                  :body (str {:ctx case-ctx
                              :step step
                              :index idx})
                  :path [::db :ctx]}}))

(zrf/defx change-step-method
  [{db :db} [_ step index new-method]]
  (let [path [::db :scenario :data :steps index]
        method (->> step
                    keys
                    (filter meths)
                    first)
        url (get step method)]
    {:db (-> db
             (update-in (into path) dissoc method)
             (assoc-in (into path [new-method]) url))}))

(zrf/defx change-url [{db :db} [_ index method value]]
  {:db (-> db
           (assoc-in (into [::db :scenario :data :steps index method]) value))})

(def default-step-style
  (c :flex :flex-col [:mb 2] [:p 2] [:bg :gray-200]))

(defn step-method [step index]
  (let [step-method (->> step
                            keys
                            (filter meths)
                            first)]
    [:select {:type "select"
              :on-change #(zrf/dispatch [::change-step-method step index (-> % .-target .-value keyword)])
              :value step-method}
     (for [meth meths]
       ^{:key meth}
       [:option {:value meth} (name meth)])]))

(defn response-view [index result]
  (if result
    [:div
     "Response"
     [zf-editor [::db :ctx :data :stresty/step-results index :response]]]
    )
  )

(defmethod render-step 'stresty/http-step [step index result]
  (let [method (first (filter meths (keys step)))]
    [:div {:class default-step-style}
     [:div {:class (c :flex :flex-row :items-)}
      [step-method step index]
      [:div {:class (c :w-full)}
       [input {:placeholder "URL"
               :default-value (get step method)
               :on-change #(rf/dispatch [::change-url index method (-> % .-target .-value)])}]]
      [zf-button {:on-click [::run-step step index]} "Run"]]
     (if-let [body (:body step)]
       [:div
        [zf-editor [::db :scenario :data :steps index :body]]])
     [response-view index result]
      ]))

(defmethod render-step 'stresty.aidbox/truncate-step [step index result]
  [:div {:class default-step-style}
   [:div {:class (c :flex :flex-row)}
    "TRUNCATE " (str (:truncate step))
    [zf-button {:on-click [::run-step step index]} "Run"]]
   [response-view index result]])

(defmethod render-step 'stresty.aidbox/sql-step [step index result]
  [:div {:class default-step-style}
   [:div {:class (c :flex :flex-row)}
    [zf-button {:on-click [::run-step step index]} "Run"]]
   [zf-editor [::db :scenario :data :steps index :sql]]
   [response-view index result]]
  )

(defmethod render-step 'stresty.aidbox/desc-step [step]
  [:div (:description step)]
  )

(defmethod render-step :default [step]
  [:pre (str step)])

(zrf/defview view [scenario case-ctx]
  [:div {:class (c [:p 6])}

   [:div {:on-click #(rf/dispatch [create-ctx])} "Create CTX"]
   [:pre (str case-ctx)]


   [:h1 {:class (c :text-2xl [:mb 2])} (:title scenario)]
   [:div {:class (c [:mb 6])} (:desc scenario)]

   (for [[idx step] (map-indexed #(vector %1 %2) (:steps scenario))]
     ^{:key idx}
     [render-step step idx (get-in case-ctx [:stresty/step-results idx])])])

(pages/reg-page index view)
