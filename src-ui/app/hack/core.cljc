(ns app.hack.core
  (:require [app.pages :as pages]
            [stylo.core :refer [c]]
            [re-frame.core :as rf]
            [zframes.re-frame :as zrf]
            [app.routes :refer [href]]
            [app.scenario.editor]
            [app.hack.interop :as interop]
            [app.hack.codemirror]
            [clojure.string :as str]))

(defn can-save-to-db [o]
  (let [size (atom 0)
        max-size (* 1024 1024)]
    (clojure.walk/postwalk
     (fn [e]
       (when (string? e)
         (swap! size + (* 2 (count e)))
         )) o)
    (< @size max-size)))

(defn enrich-with-link [url o]
  (clojure.walk/postwalk
   (fn [x]
     (if (and (:id x) (:resourceType x))
       (assoc x :id (str "<a style=\"border-bottom: 1px solid #40a9ff;\" target=\"_blank\" href=\"" url "/static/console.html#/rest?req=GET%20/" (:resourceType x) "/" (:id x) "" \"">" (:id x)  "</a>"))
       x))
   o))

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

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
     :http/fetch {:uri (str (aidbox-url-sub db) "/StrestyCase/" (get-in db [::db :id]))
                  :format "json"
                  :method "put"
                  :body case}}))

(zrf/defx create-step [{db :db} [_ step-type idx]]
  (println "create step" step-type)
  (let [step (cond-> {:case {:id (get-in db [::db :id]) :resourceType "StrestyCase"} :type (name step-type)})]
    {:http/fetch {:uri (str (aidbox-url-sub db) "/StrestyStep")
                  :method "post"
                  :format "json"
                  :headers {:content-type "application/json"}
                  :body step
                  :success {:event create-step-success
                            :idx idx}}}))

(zrf/defx get-steps-success [{db :db} [_ {data :data :as resp}]]
  (if (zero? (count data))
    {:dispatch [create-step :request 0]}
    {:db (assoc-in db [::db :steps] (reduce (fn [steps step] (assoc steps (:id step) step)) {} data))}))

(zrf/defx get-steps [{db :db} _]
  {:http/fetch {:uri (str (aidbox-url-sub db) "/StrestyStep")
                :params {:.case.id (get-in db [::db :id])}
                :format "json"
                :unbundle true
                :headers {:content-type "application/json"}
                :success {:event get-steps-success}}})

(zrf/defx create-case [{db :db} _]
  {:http/fetch {:uri (str (aidbox-url-sub db) "/StrestyCase/" (get-in db [::db :id]))
                :method "put"
                :format "json"
                :success {:event get-steps}
                :body {:type "tutorial" :steps []}
                :path [::db :case]}})

(zrf/defx get-or-create-case [{db :db} _]
  {:http/fetch {:uri (str (aidbox-url-sub db) "/StrestyCase/" (get-in db [::db :id]))
                :format "json"
                :error {:event create-case}
                :success {:event get-steps}
                :path [::db :case]}})

(zrf/defs page-sub [db] (get db ::db))

(zrf/defs steps [db]
  (get-in db [::db :steps]))

(zrf/defs stresty-case [db]
  (get-in db [::db :case :data]))

(zrf/defx update-step-value [{db :db} [_ step-id field value]]
  {:db (assoc-in db [::db :steps step-id field] value)})

;; create Entities
;; get cases
;; get-or-create-case

(zrf/defx init-failed [{db :db} _]
  (println "ERROR: failed init process"))

(zrf/defx ctx
  [{db :db} [_ phase params]]
  (let [url (aidbox-url-sub db)]
    (cond
      (= :init phase)
      {:db (-> db
               (assoc-in [::db :id] (:id params)))
       :http/fetch {:uri (str url "/")
                    :method "post"
                    :format "json"
                    :headers {:content-type "application/json"}
                    :body setup-data
                    :path [::db :config-resp]
                    :error {:event init-failed}
                    :success {:event :http/fetch
                              :uri (str url "/StrestyCase")
                              :params {:_sort "-lastUpdated"}
                              :format "json"
                              :unbundle true
                              :path [::db :cases]
                              :error {:event init-failed}
                              :success {:event get-or-create-case}}}})))

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
                         (str/split #"\n")
                         first
                         (str/split #" ")
                         first
                         str/lower-case
                         keyword)
              uri (-> content
                      (str/split #"\n")
                      first
                      (str/split #" ")
                      second)
              body (-> content
                       (str/split #"\n\n"))
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

(zrf/defx on-exec-step [{db :db} [_ {status ::status step-id :step-id data :data response :response}]]
  (let [step (-> (get-in db [::db :steps step-id])
                 (assoc-in [:status-code] (.-status response))
                 (assoc-in [:status] status)
                 (assoc-in [:result] data))]
    {:db (assoc-in db [::db :steps step-id] step)
     :http/fetch {:uri (str (aidbox-url-sub db) "/StrestyStep/" step-id)
                  :method "put"
                  :format "json"
                  :body (if (can-save-to-db data)
                          step
                          (assoc step :result {:message "Response too large to save in DB"}))}}))


(zrf/defx exec-step [{db :db} [_ step-id]]
  (let [step (get-in db [::db :steps step-id])
        http-fetch (get-http-fetch-for-step {:url (aidbox-url-sub db)} step)
        on-complete {:event on-exec-step
                     :step-id step-id}]
    {:http/fetch (merge http-fetch
                        {:success (assoc on-complete ::status "ok")
                         :error (assoc on-complete ::status "error")})}))

(defn render-step [step]
  ^{:key (:id step)}
  (let [step-type (step-type step)
        mode (cond (= "sql" step-type)
                   "sql"
                   (= "http" step-type)
                   "yaml"
                   :else
                   "javascript")]
    [:div.step
     [:style ".step .CodeMirror {min-height: 150px; height: auto;}"]

     [app.hack.codemirror/input
      [::db :steps (:id step) (keyword (:type step))]
      {"extraKeys" {"Ctrl-Enter" #(rf/dispatch [exec-step (:id step)])}
       :lineNumbers false
       :placeholder "wow"
       :mode mode}]]))

(defn render-sql-result-table [url step]
  (when (vector? (:result step))
    (let [{:keys [step-id result]} step
          ths (keys (first result))
          style (c [:border :gray-400] [:p 2])]
      [:table {:class [(c :w-full :border-collapse) style] }
       [:thead
        [:tr 
         (map-indexed (fn [i e]
                        ^{:key (str step-id "-th-" i)}
                        [:th {:class style} e]) ths)]]
       [:tbody
        (map-indexed (fn [idx e]
               ^{:key (str step-id "-tr-" idx)}
               [:tr 
                (map-indexed (fn [idx-td e]
                       ^{:key (str step-id "-tr-" idx "-td-" idx-td)}
                       [:td {:class style}
                              (if (or (seq? e) (coll? e))
                                [:pre {:dangerouslySetInnerHTML {:__html (interop/to-yaml (enrich-with-link url e))}}]
                                [:div {:dangerouslySetInnerHTML {:__html e}}])
                              ]) (vals e))
                ]) result)]])))

(defn render-result [url step]
  (let [show? (zrf/ratom true)
        render-type (zrf/ratom nil)]
    (fn [url step]
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
        (when result
          [:<>           
           [:div (when (seq result)
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
           [:div {:class [(c [:space-y 2] [:pl 2]) (if is-ok (c [:border-l :green-500]) (c [:border-l :red-500]))]}
            [:span {:class (if is-ok (c [:text :green-500]) (c [:text :red-500]))}
             (str "Status: " (:status-code step))]
            (if (empty? result)
              [:span "Empty result"]
              (let [result (enrich-with-link url result)]
                (if @show?
                      ;; [:div {:dangerouslySetInnerHTML {:__html "<a>wow</a>"}}]

                  (case @render-type
                    :table
                    [render-sql-result-table url step]
                    :yaml
                    [:pre {:dangerouslySetInnerHTML {:__html (interop/to-yaml result)}}]
                    :json
                    [:pre {:dangerouslySetInnerHTML {:__html (interop/to-json result)}}]
                    :edn
                    [:pre {:dangerouslySetInnerHTML {:__html (interop/to-pretty-edn result)}}]
                    )
                  [:pre "..."])))


            ]])))))


(zrf/defx remove-step [{db :db} [_ idx]]
  (let [case (get-in db [::db :case :data])
        case (update case :steps (fn [steps]
                                   (into
                                    (vec (take idx steps))
                                    (drop (inc idx) steps))))]
    {:http/fetch {:uri (str (aidbox-url-sub db) "/StrestyCase/" (get-in db [::db :id]))
                  :method "put"
                  :format "json"
                  :body case
                  :path [::db :case]}}))

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
       [:input {:class (c [:ml 2]) :type "button" :value "Init" :on-click #(rf/dispatch [ctx :init])}]])])




(zrf/defview view [stresty-case steps aidbox-url aidbox-auth-header]
  [:div {:class (c [:grid] [:bg :gray-100] [:m-auto] [:p 2] {:grid-template-columns "1fr 7fr"})}
   [:div
    [:h1 {:class (c :text-lg)} "Researcher's Console"]
    [:a {:href (href "hack" (rand-str 10) {:url aidbox-url :auth_header aidbox-auth-header})} "New Console"]]
   [:div

    [config-view]

    (let [left-css (c :font-light [:p 1] [:text :gray-600] [:text-right])
          right-css (c [:pl 2] [:border-l :gray-600])]
      (for [[idx step-id] (map-indexed (fn [idx step] [idx (:id step)]) (:steps stresty-case))]
        (if-let [step (get steps step-id)]
          ^{:key step-id}
          [:div
           [:div {:class (c :grid [:py 1] {:grid-template-columns "70px 1fr"})}
            [:div {:class left-css}
             [:div (step-type step)]
             (when (< 1 (count (:steps stresty-case)))
               [:div [:a {:class (c [:hover [:text :red-500]]) :on-click #(rf/dispatch [remove-step idx])} [:i.fas.fa-trash {:class (c [:text :red-300])}]]])]

            [:div.comment {:class right-css}
             [:style ".comment .CodeMirror {height: auto;}"]
             [app.hack.codemirror/input
              [::db :steps (:id step) :comment]
              {"extraKeys" {"Ctrl-Enter" #(rf/dispatch [exec-step (:id step)])}
               :lineNumbers false
               :placeholder "Add comment here..."
               :mode "markdown"
               :theme "comment"}]]
            [:div {:class left-css}]
            [:div {:class [right-css]}
             [render-step step]]
            (when (:result step)
              [render-result aidbox-url step])]
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

          [:div "loading..."])))]

   ])

(pages/reg-page ctx view)
