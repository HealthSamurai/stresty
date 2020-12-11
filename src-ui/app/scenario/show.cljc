(ns app.scenario.show
  (:require [re-frame.core :as rf]
            [app.routes :refer [href]]
            [stylo.core :refer [c]]
            [app.pages :as pages]
            [zframes.re-frame :as zrf]
            [anti.dropdown-menu :refer [zf-dropdown-menu]]
            [anti.button :refer [zf-button]]
            [anti.input :refer [input]]
            [anti.util :refer [block]]
            [reagent.core :as r]
            #?(:cljs [reagent.dom :as dom])
            #?(:cljs [app.scenario.editor :refer [zf-editor]])
            [markdown-to-hiccup.core :as md]
            [cljs.pprint :as pp]
            [clojure.string :as str]))

(def step-types
  [{:value 'stresty/http-step :display "HTTP"}
   {:value 'stresty.aidbox/desc-step :display "Text"}
   {:value 'stresty.aidbox/truncate-step :display "Truncate"}
   {:value 'stresty.aidbox/sql-step :display "SQL"}
   ]
  )

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
                     :path [::db :ctx]
                     :success {:event ::create-editors-state}}]})
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

(zrf/defx create-editors-state [{db :db} _]
  (let [steps (get-in db [::db :scenario :data :steps])
        editors-state (vec (map (fn [_] {:editing false}) steps))
        _ (prn editors-state)]
    {:db (-> db
             (assoc-in [::db :editors-state] editors-state))}))

(zrf/defs scenario [db]
  (get-in db [::db :scenario :data]))

(zrf/defs case-ctx [db]
  (get-in db [::db :ctx :data]))

(zrf/defs editors-state [db]
  (get-in db [::db :editors-state]))

(defmulti render-step (fn [step] (:type step)))

(def meths #{:GET :POST :PUT :DELETE :HEAD :PATCH :OPTION})
(def body-meths #{:POST :PUT :PATCH})

(def new-step {:type 'stresty/http-step
               :POST "/Patient"
               :body {:id "new-patient"}
               :match {:status 201}})

(update-in {:steps [{} {} {:haha 'haha}]} [:steps 1] (fn [_] nil))

(zrf/defx run-step
  [{db :db} [_ step idx]]
  (let [case-ctx (get-in db [::db :ctx :data])]
    {:dispatch [:http/fetch {:uri (str "/run-step")
                             :method "post"
                             :format "edn"
                             :body (str {:ctx case-ctx
                                         :step step
                                         :index idx})
                             :path [::db :ctx]}]
     :db (update-in db [::db :ctx :data :stresty/step-results idx] (fn [_] nil))}))

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

(defn insert-at-index [coll element index]
  (let [[before after] (split-at (+ 1 index) coll)]
    (concat before [element] after)))

(defn delete-at [coll index]
  (concat (subvec coll 0 index)
          (subvec coll (+ 1 index)))
  )

(zrf/defx add-step
 [{db :db} [_ index]]
  (let [steps (get-in db [::db :scenario :data :steps])
        ed-state (get-in db [::db :editors-state])
        results (get-in db [::db :ctx :data :stresty/step-results])
        steps* (vec (insert-at-index steps new-step index))
        ed-state* (vec (insert-at-index ed-state {:editing true} index))
        results* (vec (insert-at-index results nil index))
        ]
    {:db (-> db
             (assoc-in [::db :scenario :data :steps] steps*)
             (assoc-in [::db :editors-state] ed-state*)
             (assoc-in [::db :ctx :data :stresty/step-results] results*))}))

(zrf/defx edit-step
  [{db :db} [_ index]]
  (let [current-state (get-in db [::db :editors-state index :editing])]
    {:db (assoc-in db [::db :editors-state index :editing] (not current-state))}))

(zrf/defx export-case [{db :db} _]
  (let [case (get-in db [::db :scenario :data])
        blob (js/Blob. [(-> case
                            (pp/write :pretty true :right-margin 60)
                            with-out-str
                            (str/replace "\\n" "\n"))]
                       #js {:type "application/edn"})
        link (.createElement js/document "a")]
    (set! (.-href link) (.createObjectURL js/URL blob))
    (.setAttribute link "download" (str (:zen/name case) ".edn"))
    (.appendChild (.-body js/document) link)
    (.click link)
    (.removeChild (.-body js/document) link)
    
    {:db db}
    ))

(zrf/defx delete-step
  [{db :db} [_ index]]
  (let [steps (get-in db [::db :scenario :data :steps])
        steps* (vec (delete-at steps index))
        ed-state (get-in db [::db :editors-state])
        ed-state* (vec (delete-at ed-state index))
        results (get-in db [::db :ctx :data :stresty/step-results])
        results* (vec (delete-at results index))]
    {:db (-> db
             (assoc-in [::db :scenario :data :steps] steps*)
             (assoc-in [::db :editors-state] ed-state*)
             (assoc-in [::db :ctx :data :stresty/step-results] results*))}))

(defn step-controls [step index editing]
  [:div {:class (c :flex :flex-col :absolute [:right "100%"] :items-end)}
   [:div {:class (c :flex :flex-row :items-start)}
    [zf-button {:on-click [::add-step index]
                :type "text"} [:i.fas.fa-plus]]
    
    [zf-button {:on-click [::delete-step index]
                :type "text"
                :class (c [:text :red-300])} [:i.fas.fa-trash]]]
   [:div {:class (c :flex :flex-row :items-end)}
    (if (not (= (:type step) 'stresty.aidbox/desc-step))
      [zf-button {:on-click [::run-step step index]
                  :type "text"
                  :class (c [:text :green-300])} [:i.fas.fa-play]])
    [zf-button {:on-click [::edit-step index]
                :type "text"}
     (if editing
       [:i.fas.fa-save]
       [:i.fas.fa-pencil-alt])]
    ]])

(def bg-color :red-300)

(identity bg-color)

(defn default-step-style [result]
  (prn (:errors result))
  (let [bg-class (cond
                   (nil? result)
                   (c {:background-color "#EBECED"})
                   (seq (:errors result))
                   (c [:bg :red-200])
                   :else
                   (c [:bg :green-200])
                   )]
    [(c :flex :flex-col [:mb 2] [:p 2]) bg-class]))

(defn render-step-root [step index result editor-state]
  [:div {:class (c :relative)}
   [step-controls step index (:editing editor-state)]
   (if (:editing editor-state)
     [:div {:class (default-step-style result)}
      [zf-editor [::db :scenario :data :steps index]]]
     [render-step step index result])])

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
     [zf-editor [::db :ctx :data :stresty/step-results index :response] (:errors result)]]))

(defmethod render-step 'stresty/http-step [step index result]
  (let [method (first (filter meths (keys step)))]
    [:div {:class (default-step-style result)}
     [:div {:class (c :flex :flex-row :items-)}
      [step-method step index]
      [:div {:class (c :w-full)}
       [input {:placeholder "URL"
               :default-value (get step method)
               :on-change #(rf/dispatch [::change-url index method (-> % .-target .-value)])}]]]
     (if (contains? body-meths method)
       [:div
        [zf-editor [::db :scenario :data :steps index :body]]])
     [response-view index result]
      ]))

(defmethod render-step 'stresty.aidbox/truncate-step [step index result]
  [:div {:class (default-step-style result)}
   [:div {:class (c :flex :flex-row :items-center)}
    "TRUNCATE " (str (:truncate step))]
   [response-view index result]])

(defmethod render-step 'stresty.aidbox/sql-step [step index result]
  [:div {:class (default-step-style result)}
   
   [zf-editor [::db :scenario :data :steps index :sql]]
   [response-view index result]]
  )

(defmethod render-step 'stresty.aidbox/desc-step [step]
  [:div {:class (c [:mb 2])} (-> (:description step)
               md/md->hiccup
               md/component)])

(defmethod render-step :default [step]
  [:pre (str step)])

(zrf/defview view [scenario case-ctx editors-state]
  [:div {:class (c :flex :flex-col :items-center)}
   
   [:div {:class (c [:p 6] :flex :flex-col :w-max-4xl :w-full)}
    
    #_[:div {:on-click #(rf/dispatch [create-ctx])} "Create CTX"]
    #_[:pre (str case-ctx)]


    [:h1 {:class (c :text-2xl [:mb 2])} (:title scenario)]
    [:div {:class (c [:mb 6])} (:desc scenario)]

    (for [[idx step] (map-indexed #(vector %1 %2) (:steps scenario))
          :let [result (get-in case-ctx [:stresty/step-results idx])
                editor-state (get editors-state idx)]]
      ^{:key idx}
      [render-step-root step idx result editor-state])

    [:div 
     [zf-button {:on-click [::export-case]} "Export case"]]
    
    ]])

(pages/reg-page index view)
