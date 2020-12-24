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

(zrf/defs config [db]
  (println "DEPRECATED: config")
  {:url (get-in db [:zframes.routing/db :route :params :params :url])})

(zrf/defs aidbox-url [db]
  (get-in db [:zframes.routing/db :route :params :params :url]))

(zrf/defs aidbox-auth-header [db]
  (get-in db [:zframes.routing/db :route :params :params :auth_header]))


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
  (let [url (aidbox-url-sub db)]
    {:http/fetch {:uri (str url "/")
                  :method "post"
                  :format "json"
                  :headers {:content-type "application/json"}
                  :body setup-data
                  :path [::db :config-resp]}}))

(defn step-type [step]
  (cond
    (= "request" (:type step))
    (if (and (contains?
              #{"GET" "HEAD" "POST" "PUT" "DELETE" "CONNECT" "OPTIONS" "TRACE" "PATCH"}
              (-> (or (:request step) "")
                  str/trim
                  (str/split #" " 2)
                  first
                  str/upper-case))
             (== 2 (-> (or (:request step) "")
                     str/trim
                     (str/split #"\n")
                     first
                     (str/split #" ")
                     count)))
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

(zrf/defview config-view [aidbox-url aidbox-auth-header]
  [:div
   (let [input-cls (c [:border] [:w 50] [:ml 1] [:py 0.5] [:px 2])]
      [:div {:class (c [:p 2])}
       [:span
        [:span "Aidbox URL:"]
        [:input {:class [input-cls] :value aidbox-url :on-change #(rf/dispatch [:zframes.routing/merge-params {:url (.-value (.-target %))}])}]]
       [:span
        [:span {:class (c [:ml 2])} "Auth Header:"]
        [:input {:class [input-cls] :value aidbox-auth-header :on-change #(rf/dispatch [:zframes.routing/merge-params {:auth_header (.-value (.-target %))}])}]]
       [:input {:type "button" :value "Submit" :on-click #(rf/dispatch [setup-aidbox])}]
       [:input {:class (c [:ml 2]) :type "button" :value "Init" :on-click #(rf/dispatch [get-or-create-case])}]])])


(zrf/defx create-step-success [{db :db} [_ {idx :idx step :data}]]
  (let [case
        (if (= :last idx)
          (update (get-in db [::db :case :data]) :steps conj {:id (:id step) :resourceType "StrestyStep"})
          (update (get-in db [::db :case :data]) :steps (fn [steps]
                                                          (let [position (inc idx)]
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

;; create Entities
;; get cases
;; get-or-create-case


(zrf/defx ctx
  [{db :db} [_ phase params]]
  (cond
    (= :init phase)
    {:db (-> db
             (assoc-in [::db :id] (:id params))
             (assoc-in [::db :config] {:url "https://little.aidbox.app"}))
     :dispatch [get-or-create-case]}))

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
  (let [step (-> (get-in db [::db :steps step-id])
                 (assoc-in [:status] status)
                 (assoc-in [:result] data))]
    {:db (assoc-in db [::db :steps step-id] step)
     :http/fetch {:uri (str (get-in db [::db :config :url]) "/StrestyStep/" step-id)
                  :method "put"
                  :format "json"
                  :body (dissoc step :result)}}))


(zrf/defx exec-step [{db :db} [_ step-id]]
  (let [step (get-in db [::db :steps step-id])
        http-fetch (get-http-fetch-for-step (get-in db [::db :config]) step)
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


(defn render-sql-result-table [step]
  (when (vector? (:result step))
    (let [{:keys [step-id result]} step
          ths (keys (first result))
          style (c [:border :black] [:p 2])]
      [:table {:class [(c :w-full :border-collapse) style] }
       [:thead
        [:tr 
         (map-indexed (fn [i e]
                        ^{:key (str step-id "-th-" i)}
                        [:th {:class style} e]) ths)]
        ]
       [:tbody
        (map-indexed (fn [idx e]
               ^{:key (str step-id "-tr-" idx)}
               [:tr 
                (map-indexed (fn [idx-td e]
                       ^{:key (str step-id "-tr-" idx "-td-" idx-td)}
                       [:td {:class style}
                              (if (or (seq? e) (coll? e))
                                [:pre (interop/to-yaml e)]
                                e)
                              ]) (vals e))
                ]) result)]])))

(defn render-result [step]
  (let [show? (zrf/ratom true)
        render-type (zrf/ratom nil)]
    (fn [step]
      (let [is-ok (= (:status step) "ok")
            type (step-type step)
            result (:result step)
            allowed-render-types (cond->> [:yaml :json :edn]
                                   (and (= type "sql")
                                        (sequential? result))
                                   (cons :table))]
        (if (not @render-type)
          (reset! render-type (first allowed-render-types))
          (if (-> allowed-render-types
                  set
                  (contains? @render-type)
                  not)
            (reset! render-type (first allowed-render-types)))) 
        (when (:result step)
          [:<>           
           [:div (when (:result step)
                   [:div {:class (c :flex :flex-col [:space-y 4] :text-right [:pr 1] [:text :gray-600] :font-light)}
                    [:a {:on-click (fn [] (swap! show? not))} (if @show? "hide" "show")]
                    (when @show?
                      [:div {:class (c :flex :flex-col)}
                       (for [r allowed-render-types]
                         ^{:key (str (:id step) "-" (name r))}
                         [:a {:class [(c :cursor-pointer)
                                      (when (= @render-type r)
                                        (c :underline)
                                        )]
                              :on-click (fn [] (reset! render-type r))} (name r)]
                         )
                       ])
                    ])]
           
           [:div {:class (if is-ok (c [:border-l :green-400]) (c [:border-l :red-400]))}
            (if @show?
              (case @render-type
                :table
                [render-sql-result-table step]
                :yaml
                [:pre (interop/to-yaml result)]
                :json
                [:pre (interop/to-json result)]
                :edn
                [:pre (interop/to-pretty-edn result)]
                )
              [:pre "..."])


            ]])))))


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
  [:div {:class (c [:grid] [:bg :gray-100] [:m-auto] [:p 2] {:grid-template-columns "1fr 7fr"})}
   ;; [:style ".CodeMirror {background-color: #f7fafc;}"]
   [:div
    [:h1 {:class (c :text-lg)} "Researcher's Console"]]
   [:div
    [config-view]

    [:div {:class (c [:p 2])}
     [:input {:type "button" :value "Add step" :on-click #(rf/dispatch [create-step :request :last])}]]

    (let [left-css (c :font-light [:p 1] [:text :gray-600] [:text-right])
          right-css (c [:pl 8] [:border-l :gray-600])]
      (for [[idx step-id] (map-indexed (fn [idx step] [idx (:id step)]) (:steps stresty-case))]
        (if-let [step (get steps step-id)]
          ^{:key step-id}
          [:div
           [:div {:class (c :grid [:py 1] {:grid-template-columns "70px 1fr"})}
            [:div {:class left-css}]
            [:div {:class right-css} "comment"]
            [:div {:class left-css}
             [:div (step-type step)]
             [:div [:a {:class (c [:hover [:text :red-500]]) :on-click #(rf/dispatch [remove-step idx])} "remove"]]]
            [:div {:class [right-css (c [:p 0])]}
             [render-step step]]
            (when (:result step)
              [render-result step])]
           [:div {:class (c [:ml "62.5px"] [:mb 1])}
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

          [:div "loading..."])))]])

(pages/reg-page ctx view)
