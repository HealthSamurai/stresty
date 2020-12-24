(ns app.hack.core
  (:require [app.pages :as pages]
            [stylo.core :refer [c]]
            [re-frame.core :as rf]
            [zframes.re-frame :as zrf]
            [app.scenario.editor]
            [app.hack.interop :as interop]
            [app.hack.codemirror]
            [clojure.string :as str]))

(zrf/defs get-value [db [_ path]]
  (get-in db path))

(zrf/defd set-value [db [_ path v]]
  (assoc-in db path v))

(zrf/defx ctx
  [{db :db} [_ phase params]]
  (cond
    (= :init phase)
    {:db (-> db
             (assoc-in [::db :id] (:id params))
             (assoc-in [::db :config] {:url "https://little.aidbox.app"}))}))

(zrf/defx update-config [{db :db} [_ field value]]
  {:db (assoc-in db [::db :config field] value)})

(def setup-data
  {:resourceType "Bundle"
   :type "trasaction"
   :entry [{:request {:url "/Entity/StrestyCase" :method "PUT"}
            :resource {:type "resource"
                       :resourceType "Entity"
                       :isOpen true}}
           {:request {:url "/Entity/StrestyStep" :method "PUT"}
            :resource {:type "resource"
                       :resourceType "Entity"
                       :isOpen true}}]})

(zrf/defx setup-aidbox [{db :db} _]
  (let [config (get-in db [::db :config])]
    {:http/fetch {:uri (str (:url config) "/")
                  :method "post"
                  :format "json"
                  :headers {:content-type "application/json"}
                  :body setup-data
                  :path [::db :config-resp]}}))

(defn step-type [step]
  (cond
    (= "request" (:type step))
    (if (contains?
         #{"GET" "HEAD" "POST" "PUT" "DELETE" "CONNECT" "OPTIONS" "TRACE" "PATCH"}
         (-> (or (:request step) "")
             str/trim
             (str/split #" " 2)
             first
             str/upper-case))
      "http"
      "sql")

    :else
    (:type step)))

(zrf/defx get-steps-success [{db :db} [_ {data :data :as resp}]]
  {:db (assoc-in db [::db :steps] (reduce (fn [steps step] (assoc steps (:id step) step)) {} data))})

(zrf/defx get-steps [{db :db} _]
  {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyStep")
                :params {:.case.id (get-in db [::db :id])}
                :format "json"
                :unbundle true
                :headers {:content-type "application/json"}
                :success {:event get-steps-success}}})

(zrf/defx create-case [{db :db} _]
  {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyCase/" (get-in db [::db :id]))
                :method "put"
                :format "json"
                :headers {:content-type "application/json"}
                :success {:event get-steps}
                :body {:type "tutorial" :steps []}
                :path [::db :case]}})

(zrf/defx get-or-create-case [{db :db} _]
  {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyCase/" (get-in db [::db :id]))
                :format "json"
                :headers {:content-type "application/json"}
                :error {:event create-case}
                :success {:event get-steps}
                :path [::db :case]}})



(zrf/defs page-sub [db] (get db ::db))

(zrf/defs config [db] (get-in db [::db :config]))

(zrf/defview config-view [config]
  [:div
    (let [input-cls (c [:border])]
      [:div {:class (c [:p 2])}
       [:div
        [:span "Aidbox URL"]
        [:input {:class [input-cls] :value (:url config) :on-change #(rf/dispatch [update-config :url (.-value (.-target %))])}]]
       [:div
        [:span "Auth header"]
        [:input {:class [input-cls] :value (:auth config) :on-change #(rf/dispatch [update-config :auth (.-value (.-target %))])}]]
       [:input {:type "button" :value "Submit" :on-click #(rf/dispatch [setup-aidbox])}]
       [:input {:class (c [:ml 2]) :type "button" :value "Init" :on-click #(rf/dispatch [get-or-create-case])}]
       ])])


(comment

  (let [x '(1 2 3 4 5 6)]
    (into
     (into (vec (take 2 x)) [0])
     (drop 2 x)
     ))


  )


(zrf/defx create-step-success [{db :db} [_ {idx :idx step :data}]]
  (let [case
        (if (= :last idx)
          (update (get-in db [::db :case :data]) :steps conj {:id (:id step) :resourceType "StrestyStep"})
          (update (get-in db [::db :case :data]) :steps (fn [steps]
                                                          (let [position (inc idx)]
                                                            (println "insert into" position)
                                                            (prn (take position steps))
                                                            (into
                                                             (into
                                                              (vec (take position steps))
                                                              [{:id (:id step) :resourceType "StrestyStep"}])
                                                             (drop position steps))))))]
    {:db (-> db
             (assoc-in [::db :steps (:id step)] step)
             (assoc-in [::db :case :data] case))
     :http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyCase/" (get-in db [::db :id]))
                  :format "json"
                  :method "put"
                  :body case}}))

(zrf/defx create-step [{db :db} [_ step-type idx]]
  (println "create step" step-type)
  (let [step (cond-> {:case {:id (get-in db [::db :id]) :resourceType "StrestyCase"} :type (name step-type)})]
    {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyStep")
                  :method "post"
                  :format "json"
                  :headers {:content-type "application/json"}
                  :body step
                  :success {:event create-step-success
                            :idx idx}}}))


(zrf/defs steps [db]
  (get-in db [::db :steps]))

(zrf/defs stresty-case [db]
  (get-in db [::db :case :data]))

(zrf/defx update-step-value [{db :db} [_ step-id field value]]
  {:db (assoc-in db [::db :steps step-id field] value)})


(defn get-http-fetch-for-step [config step]
  (cond
    (= "request" (:type step))
    (let [t (step-type step)]
      (cond
        (= "sql" t)
        {:uri (str (:url config) "/$sql")
         :method "post"
         :format "json"
         :body [(:request step)]}

        (= "http" t)
        (let [content (:request step)
              method (-> content
                         (str/split "\n")
                         first
                         (str/split " ")
                         first
                         str/lower-case
                         keyword)
              uri (-> content
                      (str/split "\n")
                      first
                      (str/split " ")
                      second)
              body (-> content
                       (str/split "\n\n"))
              ]
          (cond-> {:uri (str (:url config) uri)
                   :method method
                   :format "json"}
            (#{:post :put :patch} method)
            (assoc :body (interop/from-yaml (last body)))
            ))

        ))

    (= "sql" (:type step))
    {:uri (str (:url config) "/$sql")
     :method "post"
     :format "json"
     :body [(:sql step)]}

    (= "http" (:type step))
    (let [content (:http step)
          method (-> content
                     (str/split "\n")
                     first
                     (str/split " ")
                     first
                     str/lower-case
                     keyword)
          uri (-> content
                  (str/split "\n")
                  first
                  (str/split " ")
                  second)
          body (-> content
                   (str/split "\n\n"))
          ]
      (cond-> {:uri (str (:url config) uri)
               :method method
               :format "json"}
        (#{:post :put :patch} method)
        (assoc :body (interop/from-yaml (last body)))
        ))
    :else
    (throw (ex-info "no such step type" {}))))

(zrf/defx on-exec-step [{db :db} [_ {status ::status step-id :step-id data :data}]]
  (println "DATA: " data)
  (let [step (-> (get-in db [::db :steps step-id])
                 (assoc-in [:status] status)
                 (assoc-in [:result] data))]
    {:db (assoc-in db [::db :steps step-id] step)
     :http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyStep/" step-id)
                  :method "put"
                  :format "json"
                  :body (dissoc step :result)}}))


(zrf/defx exec-step [{db :db} [_ step-id]]
  (println "exec" (get-in db [::db :steps step-id]))
  (let [step (get-in db [::db :steps step-id])
        http-fetch (get-http-fetch-for-step (get-in db [::db :config]) step)
        _ (prn "http" http-fetch)
        on-complete {:event on-exec-step
                     :step-id step-id}]
    {:http/fetch (merge http-fetch
                        {:success (assoc on-complete ::status "ok")
                         :error (assoc on-complete ::status "error")})}))

(defn render-step [step]
  ^{:key (:id step)}
  [app.hack.codemirror/input
   [::db :steps (:id step) (keyword (:type step))]
   {"extraKeys" {"Ctrl-Enter" #(rf/dispatch [exec-step (:id step)])}}])




(defn render-sql-result-table [result]
  (let [ths (keys (first result))
        style (c [:border :black] [:p 2])]
    [:table {:class [(c :w-full) style] }
     [:thead
      (map (fn [e] [:th {:class style}
                    e]) ths)
      ]
     [:tbody
      (map (fn [e] [:tr
                    (map (fn [e] [:td {:class style}
                                  (if (or (seq? e) (coll? e))
                                    [:pre (interop/to-yaml e)]
                                    e)
                                  ]) (vals e))
                    ]) result)
      ] 
     ]
    
    )
  )

(defn render-result [step]
  (let [show? (zrf/ratom true)
        {:keys [type result]} step]
    (println "Result: " result)
    (fn []
      (let [is-ok (= (:status step) "ok")
            class (if is-ok
                    (c [:pl 2] [:border :green-400] [:border-l 1] [:border-r 0] [:border-t 0] [:border-b 0])
                    (c [:pl 2] [:border :red-400] [:border-l 1] [:border-r 0] [:border-t 0] [:border-b 0]))]
        [:div {:class (c :grid [:py 1] {:grid-template-columns "40px 1fr"})}
         (when (:status step)
           [:<>
            [:div
             [:div (when is-ok [:a {:on-click (fn [] (swap! show? not))} (if @show? "hide" "show")])]]
            [:div {:class class}
             (if is-ok 
               (if @show?
                 (cond (= "http" type)
                       [:pre (interop/to-yaml (get step :result))]
                       (= "sql" type)
                       [render-sql-result-table result]
                       :else
                       [:div "Some result from aidbox"]
                       )
                 [:pre "..."])
               [:pre (get-in step [:result :text :div])]
      
               )]])]))))


(zrf/defx remove-step [{db :db} [_ idx]]
  (let [case (get-in db [::db :case :data])
        case (update case :steps (fn [steps]
                                   (into
                                    (vec (take idx steps))
                                    (drop (inc idx) steps))))]
    {:http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyCase/" (get-in db [::db :id]))
                  :method "put"
                  :format "json"
                  :body case
                  :path [::db :case]}}))


(zrf/defview view [stresty-case steps]
  [:div {:class (c [:grid] [:bg :gray-100] [:m-auto] [:w 300] [:p 2] {:grid-template-columns "1fr 7fr"})}
   [:div "wow"]
   [:div
    [config-view]

    [:div {:class (c [:p 2])}
     [:input {:type "button" :value "Add step" :on-click #(rf/dispatch [create-step :request :last])}]]

    (for [[idx step-id] (map-indexed (fn [idx step] [idx (:id step)]) (:steps stresty-case))]
      ^{:key step-id}
      (if-let [step (get steps step-id)]
        [:<>
         [:div {:class (c :grid [:py 1] {:grid-template-columns "40px 1fr"})}
          [:div {:class (c :font-light [:p 1] [:text :gray-600] [:text-right])}
           [:div (step-type step)]
           [:div
            [:a {:class (c [:hover [:text :red-500]]) :on-click #(rf/dispatch [remove-step idx])} "del"]]]
          [:div
           {:class (c [:pl 2] [:border :gray-600] [:border-l 1] [:border-r 0] [:border-t 0] [:border-b 0])}
           [render-step step]]]
         (if (:result step)
           [render-result step])
         [:div {:class (c [:ml "32.5px"] [:mb 1])}
          [:svg {:viewBox "0 0 15 15" :x 0 :y 0 :width 15 :height 15 :stroke "currentColor"
                 :on-click #(rf/dispatch [create-step :request idx])
                 :class (c
                         :inline-block
                         :cursor-pointer
                           [:hover
                            [:text :green-500]]
                           [:active
                            [:text :blue-500]]
                           {:stroke-width 1 :stroke-linecap "round"})}
           [:line {:x1 7.5 :x2 7.5 :y1 2.5 :y2 12.5}]
           [:line {:y1 7.5 :y2 7.5 :x1 2.5 :x2 12.5}]]]]
        [:div "loading..."]))]])



(pages/reg-page ctx view)
