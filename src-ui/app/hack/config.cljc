(ns app.hack.config
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
            ))

(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})

(defonce cms (r/atom {}))

(zrf/defx ctx
  [{db :db} [_ phase params]]
  )

(zrf/defx run-step
  [{db :db} [_ index]]
                                        ; TODO: run case on server
  (let [current-case-id (:current-case db)]
    {:db (-> db
             (assoc-in [:cases current-case-id :steps index :result]
                       {:run true
                        :response {:status 200
                                   :body {:message "Access Denied"}}}))}))

(zrf/defx change-value
  [{db :db} [_ path value]]
  (let [current-case-id (:current-case db)]
    {:db (-> db
             (assoc-in (into [:cases current-case-id :steps] path) value))}))

(dissoc {:step {:PATCH "/Patient" :id :update-patient}} [:step :PATCH])

(zrf/defx change-step-method
  [{db :db} [_ step index new-method]]
  (let [current-case-id (:current-case db)
        path [:cases current-case-id :steps index]
        method (->> step
                    keys
                    (filter meths)
                    first)
        url (get step method)]
    {:db (-> db
             (update-in (into path) dissoc method)
             (assoc-in (into path [new-method]) url))}))

(zrf/defx change-url
  [{db :db} [_ index method new-url]]
  (let [current-case-id (:current-case db)
        path [:cases current-case-id :steps index method]]
    {:db (-> db
             (assoc-in path new-url))}))

(zrf/defx add-step
  [{db :db} [_ _]]
  (let [current-case-id (:current-case db)]
    {:db (-> db
             (update-in [:cases current-case-id :steps] conj {:id :new-step
                                                              :POST "/Users"
                                                              :body {:id "new-patient"}
                                                              :editing true}))}))
(vec (remove #(= (:id %) :new-step) [{:id :create-patient} {:id :new-step}]))

(zrf/defx edit-step
  [{db :db} [_ index]]
  (let [current-case-id (:current-case db)
        path [:cases current-case-id :steps index :editing]
        editing (get-in db path)]
    {:db (-> db
             (assoc-in path (not editing)))}))

(zrf/defx delete-step
  [{db :db} [_ step]]
  (let [current-case-id (:current-case db)
        path [:cases current-case-id :steps]
        steps (get-in db path)
        steps* (vec (remove #(= (:id %) (:id step)) steps))
        ]
    {:db (-> db
             (assoc-in path steps*))}))

(zrf/defs current-case
  [db _]
  (let [current-case-id (:current-case db)]
    (get-in db [:cases current-case-id])))

(defn editor [step path read-only step-index]
  (let [data @(rf/subscribe [::current-case])
        value (get-in step path)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [el (dom/dom-node this)
              component-id (str "result-" (:id step))
              monaco (monaco/editor.create el
                                           #js {:tabSize 2
                                                :language "clojure"})]
          (.setValue monaco (with-out-str (cljs.pprint/pprint value)))
          ))

      :reagent-render
      (fn [stresty-case]
        [:div {:class (c [:h 100])}]
        )
      }
     ))
  )

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

(defn step-preview [step index]
  (let [method (->> step
                    keys
                    (filter meths)
                    first)
        url (get step method)
        body (:body step)]
    [:div {:class (c [:mt 2])}
     [:span {:class (c :text-base)}(:desc step)]
     [:div {:class (c :flex :flex-col [:bg :gray-200] [:p 1])}
      [:div {:class (c :flex :flex-row :justify-between [:space-x 2] [:mb 1])}

       [step-method step index]
       [:div {:class (c :w-full)}
        [input {:placeholder "URL"
                :default-value url
                :on-change #(rf/dispatch [::change-url index method (-> % .-target .-value)])
                }]]
       [zf-button {:class (c [:bg :yellow-500])
                   :type "primary"
                   :on-click [::run-step index]} "Run"]
       [zf-button {:class (c [:bg :black])
                   :type "primary"
                   :on-click [::edit-step index]} "Raw"]
       [zf-button {:class (c [:bg :red-600])
                   :type "primary"
                   :on-click [::delete-step step]}
        [:i.far.fa-trash-alt]]]

      (when (-> body
                nil?
                not)
        [editor step [:body] false index])

      (when (-> step
                (get-in [:result :run]))
        [:div
         [:div {:class (c [:h 2])}]
         [editor step [:result :response] true index]])]]))

(defn step-raw-editor [step index]
  [:div {:class (c [:mt 2])}
   [:div {:class (c :flex :flex-col [:bg :gray-200] [:p 1])}
    [:div {:class (c :flex :flex-row :justify-end)}
     [zf-button {:type "primary"
                 :on-click [::edit-step index]} "Save"]]
    [editor step [] false index]
    ]
   ]
  )

(defn step-view [step index]
  (let [editing? (:editing step)]
    (if editing?
      [step-raw-editor step index]
      [step-preview step index]
      )
    ))

(zrf/defview view [current-case]
  [:div {:class (c [:p 6])}
   [:span {:class (c :text-lg :font-semibold)} (:desc current-case)]
   [:div {:class (c [:space-y 2])}
    (map-indexed (fn [i step]
                   ^{:key (:id step)}
                   [step-view step i])
                 (:steps current-case)
                 )]
   [:div {:class (c [:mt 4])}
    [zf-button {:type "primary"
                :on-click [::add-step]}
     [:i.far.fa-plus-square {:class (c [:mr 1])}]
     " Add step"]]])

(pages/reg-page ctx view)
