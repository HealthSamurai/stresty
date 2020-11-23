(ns app.reagent
  (:require [app.pages :as pages]
            [zframes.re-frame :as zrf]
            [reagent.core :as r]
            [stylo.core :refer [c]]
            [app.routes :refer [href]]))

(zrf/defx ctx
  [{db :db} [_ phase {params :params}]]
  (println "Reagent"))

(defn react-life-cycle [x y z]
  (let [local-state (r/atom {})]
    (r/create-class                 ;; <-- expects a map of functions 
     {:display-name  "my-component"      ;; for more helpful warnings & errors

      :component-did-mount               ;; the name of a lifecycle function
      (fn [this] 
        (println "component-did-mount" this)) ;; your implementation

      :component-did-update              ;; the name of a lifecycle function
      (fn [this old-argv]                ;; reagent provides you the entire "argv", not just the "props"
        (let [new-argv (rest (r/argv this))]
          (println "did update" old-argv new-argv)))

      :reagent-render        ;; Note:  is not :render
      (fn [x y z]           ;; remember to repeat parameters
        [:div {:class (c [:p 4] [:bg :green-100] :border)}
         "React lifecycle:"
         (str x " " y " " z)])})))

(defn component-with-state [_]
  (let [my-state (r/atom {})
        on-click (fn [_] (swap! my-state update :count (fn [x] (inc (or x 0)))))]
    (fn [_]
      [:div {:ref (fn [el] (println "Link to dom" el))}
       "I'm stateful"
       [:div {:class (c [:p 10] :border :cursor-pointer)
              :on-click on-click} "Click me "
        [:span {:class (c [:bg :blue-300] [:px 2]
                          [:text :white] :font-bold)} (:count @my-state)]]])))

(defn pure-component [{title :title }]
  [:<>
   [:div "Pure component"]
   [:div {:class (c [:bg :red-200] [:p 4] :border)} title]])

(defn page []
  [:div {:class (c [:p 10])}
   [:h1 {:class (c :text-xl [:mb 6])}
    "Reagent basics"]

   [:div {:class (c :flex [:space-x 6])}
    [:div [pure-component {:title "Pure Component"}]]
    [component-with-state {}]
    [react-life-cycle "x" "y" "z"]]

   ])

(pages/reg-page ctx page)
