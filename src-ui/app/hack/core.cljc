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

(zrf/defs aidbox-url [db]
  (get-in db [:zframes.routing/db :route :params :params :url]))

(zrf/defs aidbox-auth-header [db]
  (get-in db [:zframes.routing/db :route :params :params :auth_header]))

(zrf/defx aidbox-fetch [{db :db} [_ data]]
  {:http/fetch (cond-> data
                 (seq (get-in db [:zframes.routing/db :route :params :params :url]))
                 (assoc :uri (str (get-in db [:zframes.routing/db :route :params :params :url]) (:uri data)))

                 (and (get-in db [:zframes.routing/db :route :params :params :auth_header]) (seq (get-in db [:zframes.routing/db :route :params :params :auth_header])))
                 (assoc-in [:headers "authorization"] (get-in db [:zframes.routing/db :route :params :params :auth_header]))

                 true
                 (assoc :format "json"))})

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


(zrf/defx root [{db :db}]
  {:dispatch [:zframes.routing/redirect {:uri (str "/hack/" (rand-str 10))
                                         :params {:url (get-in db [:zframes.routing/db :route :params :params :url])
                                                  :auth_header (get-in db [:zframes.routing/db :route :params :params :auth_header])}}]})


(zrf/defs get-value [db [_ path]]
  (get-in db path))

(zrf/defd set-value [db [_ path v]]
  (assoc-in db path v))


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
    {:dispatch [aidbox-fetch {:uri "/"
                              :method "post"
                              :body setup-data
                              :path [::db :config-resp]}]}))

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
                                                          (let [position idx]
                                                            (into
                                                             (into
                                                              (vec (take position steps))
                                                              [{:id (:id step) :resourceType "StrestyStep"}])
                                                             (drop position steps))))))]
    {:db (-> db
             (assoc-in [::db :steps (:id step)] step)
             (assoc-in [::db :case :data] case))
     :dispatch [aidbox-fetch {:uri (str "/StrestyCase/" (get-in db [::db :id]))
                              :format "json"
                              :method "put"
                              :body case}]}))

(zrf/defx create-step [{db :db} [_ step-type idx]]
  (println "create step" step-type)
  (let [step (cond-> {:case {:id (get-in db [::db :id]) :resourceType "StrestyCase"} :type (name step-type)})]
    {:dispatch [aidbox-fetch {:uri (str "/StrestyStep")
                              :method "post"
                              :body step
                              :success {:event create-step-success
                                        :idx idx}}]}))

(zrf/defx get-steps-success [{db :db} [_ {data :data :as resp}]]
  (if (zero? (count data))
    {:dispatch [create-step :request 0]}
    {:db (assoc-in db [::db :steps] (reduce (fn [steps step] (assoc steps (:id step) step)) {} data))}))

(zrf/defx get-steps [{db :db} _]
  {:dispatch [aidbox-fetch {:uri (str "/StrestyStep")
                            :params {:.case.id (get-in db [::db :id])}
                            :format "json"
                            :unbundle true
                            :success {:event get-steps-success}}]})

(zrf/defx create-case [{db :db} _]
  {:dispatch [aidbox-fetch {:uri (str "/StrestyCase/" (get-in db [::db :id]))
                            :method "put"
                            :success {:event get-steps}
                            :error {:event 'init-failed}
                            :body {:type "tutorial" :steps []}
                            :path [::db :case]}]})

(zrf/defx get-or-create-case [{db :db} _]
  {:dispatch [aidbox-fetch {:uri (str "/StrestyCase/" (get-in db [::db :id]))
                            :format "json"
                            :error {:event create-case}
                            :success {:event get-steps}
                            :path [::db :case]}]})

(zrf/defs page-sub [db] (get db ::db))

(zrf/defs connection-status [db] (get-in db [::db :status]))

(zrf/defs steps [db]
  (get-in db [::db :steps]))

(zrf/defs stresty-case [db]
  (get-in db [::db :case :data]))

(zrf/defx update-step-value [{db :db} [_ step-id field value update?]]
  (let [step (get-in db [::db :steps step-id])
        step (assoc step field value)]
    (cond-> {:db (assoc-in db [::db :steps step-id] step)}
      update?
      (assoc :dispatch [aidbox-fetch {:uri (str "/StrestyStep/" step-id)
                                      :method "put"
                                      :body (if (can-save-to-db (:result step))
                                              step
                                              (assoc step :result {:message "Response too large to save in DB"}))}]))))

;; create Entities
;; get cases
;; get-or-create-case

(zrf/defx init-failed [{db :db} _]
  {:db (assoc-in db [::db :status] :offline)}
  )

(zrf/defx ctx
  [{db :db} [_ phase]]
  (let [case-id (get-in db [:fragment-params :id])]
    (cond
      (= :init phase)
      {:db (-> db
               (assoc-in [::db :id] case-id)
               (assoc-in [::db :status] :online))
       :dispatch [aidbox-fetch {:uri "/"
                                :method "post"
                                :body setup-data
                                :path [::db :config-resp]
                                :error {:event init-failed}
                                :success {:event get-or-create-case}}]}
      (= :params phase)
      {:db (-> db
               (update ::db dissoc :case)
               (update ::db dissoc :status))}

      (= :deinit phase)
      {:db (dissoc db ::db)})))

(defn get-http-fetch-for-step [step]
  (cond
    (= "request" (:type step))
    (let [t (step-type step)]
      (cond
        (= "sql" t)
        {:uri "/$psql"
         :method "post"
         :body {:query (:request step)}}

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
          #_(cond-> {:uri uri
                   :method method}
            (#{:post :put :patch} method)
            #_(assoc :body (interop/from-yaml (last body)))
            ))

        ))


    (= "sql" (:type step))
    {:uri "/$sql"
     :method "post"
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
      #_(cond-> {:uri (str aidbox-url uri)
               :headers {"authorization" aidbox-auth-header}

               :method method
               :format "json"}
        (#{:post :put :patch} method)
        #_(assoc :body (interop/from-yaml (last body)))
        ))
    :else
    (throw (ex-info "no such step type" {}))))

(zrf/defx on-exec-step [{db :db} [_ {status ::status step-id :step-id data :data response :response}]]
  (println "Duration: " (.get (.-headers response) "duration"))
  (let [step (-> (get-in db [::db :steps step-id])
                 (assoc-in [:status-code] (.-status response))
                 (assoc-in [:status] status)
                 (assoc-in [:result] data))]
    {:db (assoc-in db [::db :steps step-id] step)
     :dispatch [aidbox-fetch {:uri (str "/StrestyStep/" step-id)
                              :method "put"
                              :body (if (can-save-to-db data)
                                      step
                                      (assoc step :result {:message "Response too large to save in DB"}))}]}))


(zrf/defx exec-step [{db :db} [_ step-id]]
  (let [step (get-in db [::db :steps step-id])
        http-fetch (get-http-fetch-for-step step)
        on-complete {:event on-exec-step
                     :step-id step-id}]
    {:dispatch [aidbox-fetch (merge http-fetch
                                    {:success (assoc on-complete ::status "ok")
                                     :error (assoc on-complete ::status "error")})]}))

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
     [:style ".step .CodeMirror {height: auto;} .step .CodeMirror-scroll {min-height: 150px;}"]
     [app.hack.codemirror/input
      [::db :steps (:id step) (keyword (:type step))]
      {"extraKeys" {"Ctrl-Enter" #(rf/dispatch [exec-step (:id step)])}
       :lineNumbers false
       :placeholder "GET /Patient or sql"
       :mode mode}]]))

(defn render-sql-result-table [url result]
  (if (vector? result)
    (let [ths (keys (first result))
          style (c [:border :gray-400] [:p 2])]
      [:table {:class [(c :w-full :border-collapse) style] }
       [:thead
        [:tr 
         (map-indexed (fn [i e]
                        ^{:key (str  "th-" i)}
                        [:th {:class style} e]) ths)]]
       [:tbody
        (map-indexed (fn [idx e]
               ^{:key (str "tr-" idx)}
               [:tr 
                (map-indexed (fn [idx-td e]
                       ^{:key (str "tr-" idx "-td-" idx-td)}
                               [:td {:class style
                                     :valign "top"}
                        (if (or (seq? e) (coll? e))
                          [:pre #_{:dangerouslySetInnerHTML {:__html (interop/to-yaml (enrich-with-link url e))}}]
                          [:div {:dangerouslySetInnerHTML {:__html e}}])
                        ]) (vals e))
                ]) result)]])
    #_[:pre {:dangerouslySetInnerHTML {:__html (interop/to-yaml result)}}]
    ))

(defn render-result-row [result url render-type]
  (if (empty? result)
    [:span "Empty result"]
    (let [result* (enrich-with-link url result)]
      (case render-type
        "table"
        [render-sql-result-table url result]
        "yaml"
        #_[:pre {:dangerouslySetInnerHTML {:__html (interop/to-yaml result*)}}]
        "json"
        [:pre {:dangerouslySetInnerHTML {:__html (interop/to-json result*)}}]
        "edn"
        #_[:pre {:dangerouslySetInnerHTML {:__html (interop/to-pretty-edn result*)}}])))
  )

(defn render-result [url step]
  (let [render-type (or (:render-type step) "yaml")
        type (step-type step)
        result (:result step)
        allowed-render-types (cond->> ["yaml" "json" "edn"]
                               (and (sequential? result))
                               (cons "table"))
        render-type (or ((set allowed-render-types) (:render-type step))
                        (first allowed-render-types))]
    (when result
      [:<>
       [:div {:class [(c [:space-y 2] [:pl 4] [:pr 2])]}
        [:div {:class (c :flex :justify-between :items-center)}
         [:span {:class (if (= "ok" (:status step)) (c [:text :green-500]) (c [:text :red-500]))}
          (str "Status: " (:status-code step))]
         [:span {:class (c [:mr 2])}
          (for [r allowed-render-types]
            ^{:key (str (:id step) "-" r)}
            [:a {:class [(c :cursor-pointer [:mr 2])
                         (when (= render-type r)
                           (c :underline))]
                 :on-click #(rf/dispatch [update-step-value (:id step) :render-type r true])}
             r ])]]
        (cond
          (and (vector? result) (= type "sql"))
          (map-indexed
           (fn [i e]
             [:<>
              [:span {:class (c :text-sm)} (str (:query e) " [" (:duration e) "ms]")]
              [:hr]
              [render-result-row (:result e) url render-type]
              ])
           result)
          :else
          [render-result-row result url render-type])]])))


(zrf/defx remove-step [{db :db} [_ idx]]
  (let [case (get-in db [::db :case :data])
        case (update case :steps (fn [steps]
                                   (into
                                    (vec (take idx steps))
                                    (drop (inc idx) steps))))]
    {:dispatch [aidbox-fetch {:uri (str "/StrestyCase/" (get-in db [::db :id]))
                              :method "put"
                              :format "json"
                              :body case
                              :path [::db :case]}]}))

(defn add-step-button [idx]
  [:div {:class (c :relative [:top -1])}
   [:svg {:viewBox "0 0 15 15" :x 0 :y 0 :width 15 :height 15 :stroke "currentColor"
          :on-click #(rf/dispatch [create-step :request idx])
          :class (c :absolute
                    [:right "-7px"]
                    [:bottom "100%"]
                    :inline-block
                    :cursor-pointer
                    [:hover
                     [:text :green-600]
                     :rounded
                     [:bg :gray-200]]
                    [:active
                     [:text :blue-500]]
                    {:stroke-width 1 :stroke-linecap "round"})}
    [:line {:x1 7.5 :x2 7.5 :y1 2.5 :y2 12.5}]
    [:line {:y1 7.5 :y2 7.5 :x1 2.5 :x2 12.5}]]])

(zrf/defx set-active-step [{db :db} [_ step-id]]
  {:db (assoc-in db [::db :active-step] step-id)})

(zrf/defs active-step [db]
  (when-let [step-id (or (get-in db [::db :active-step]) (:id (first (get-in db [::db :case :data :steps]))))]
    (get-in db [::db :steps step-id])))


(zrf/defx patch-step [{db :db} [_ step-id field value]]
  {:dispatch [aidbox-fetch {:uri (str "/StrestyStep/" step-id)
                            :method "put"
                            :body (get-in db [::db :steps step-id])}]})

(zrf/defview view [stresty-case steps aidbox-url aidbox-auth-header active-step connection-status]
  [:div {:class (c  [:grid] [:bg :gray-100] [:m-auto])}
   [:div {:class (c [:py 3] [:px 4] [:bg :gray-300] :flex :justify-between :items-center)}
    [:div
     [:span {:class [(c :inline-block [:w "10px"] [:h "10px"] [:rounded :full] [:mr 1])
                     (cond
                       (= connection-status :online)
                       (c [:bg :green-500])
                       (= connection-status :connecting)
                       (c [:bg :yellow-500])
                       :else
                       (c [:bg :red-500]))]}]
     [:h1 {:class (c  [:pr 4] :text-lg :inline-block) } "Researcher's Console"]
     [:a {:href (href "hack" (rand-str 10) {:url aidbox-url :auth_header aidbox-auth-header})} "New Console"]]
    (let [input-cls (c [:border] [:w 60] [:ml 1] [:py 0.5])]
    [:span
     [:input {:class (c :rounded [:py 0.5] [:px 2.5] [:ml 2]) :type "button" :value "Init" :on-click #(rf/dispatch [ctx :init])}]
     [:input {:id "aidbox-url" :placeholder "Aidbox URL" :class [input-cls] :value aidbox-url :on-change #(rf/dispatch [:zframes.routing/merge-params {:url (.-value (.-target %))}])}]
     [:input {:id "aidbox-auth-header" :placeholder "Auth Header":class [input-cls] :value aidbox-auth-header :on-change #(rf/dispatch [:zframes.routing/merge-params {:auth_header (.-value (.-target %))}])}]])]

   [:div {:class (c :flex :flex-row [:py 1])}
    [:div {:class (c [:w "40vw"])}
     (for [[idx step-id] (map-indexed (fn [idx step] [idx (:id step)]) (:steps stresty-case))]
       (if-let [step (get steps step-id)]
         ^{:key step-id}
         [:div {:class (c [:my 6])}
          [add-step-button idx]
          [:div
           {:on-click #(rf/dispatch [set-active-step step-id])
            :class [(c  [:border-b :gray-400] [:border-r :gray-400]
                        [:hover [:border-b :gray-600] [:border-r :gray-600]])
                    (when (= (:id active-step) (:id step))
                      (c [:border-b :gray-600] [:border-r :gray-600]))]}
           [:div {:class (c :flex :justify-between :items-start)}
            [:div.comment {:class (c [:w "93%"])}
             [:style ".comment .CodeMirror {height: auto;}"]
             [app.hack.codemirror/input
              [::db :steps (:id step) :comment]
              {"extraKeys" {"Ctrl-Enter" #(rf/dispatch [exec-step (:id step)])}
               :lineNumbers false
               :on-change #(rf/dispatch [patch-step step-id :comment %])
               :placeholder "Add comment here..."
               :mode "markdown"
               :theme "comment"}]]
            [:div {:class (c :flex :flex-row :self-center)}
             (when (< 1 (count (:steps stresty-case)))
               [:a {:on-click #(rf/dispatch [remove-step idx])}
                [:i.fas.fa-trash {:class (c
                                          [:mx 1]
                                          [:text :red-300]
                                          [:hover [:text :red-400]])}]])
             [:a {:on-click #(rf/dispatch [exec-step (:id step)])}
              [:i.fas.fa-play {:class (c
                                       [:mx 1]
                                       [:text :green-300]
                                       [:hover [:text :green-400]])}]]]]
           [render-step step]]]
         [:div "loading..."]))
     (when stresty-case
       [add-step-button :last])]
    
    [:div {:class (c [:w "60vw"] :overflow-x-auto)}
     (when (:result active-step)
       [render-result aidbox-url active-step])]]])




(pages/reg-page ctx view)
