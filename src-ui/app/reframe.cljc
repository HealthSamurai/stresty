(ns app.reframe
  (:require [app.pages :as pages]
            [re-frame.core :as rf]
            [stylo.core :refer [c]]
            [app.routes :refer [href]]))

(rf/reg-event-fx
 ::ctx
 (fn [{db :db} [_ phase {params :params}]]
   (println "Re-frame" phase params)))

(rf/reg-sub
 ::mysub
 (fn [db _]
   (or (::click db) 0)))

(rf/reg-event-fx
 ::click
 (fn [{db :db} _]
   {:db (update db ::click (fn [x] (inc (or x 0))))}))

(rf/reg-event-fx
 ::change
 (fn [{db :db} [_ value]]
   (println "Changed" value)
   {:db (assoc db ::click #?(:cljs (js/parseInt value) :clj value))}))

(defn page []
  (let [sub (rf/subscribe [::mysub 1])]
    (fn []
      [:div {:class (c [:p 10])}
       [:h1 {:class (c :text-xl)} "re-frame"]
       [:div {:class (c :font-bold)} @sub]
       [:button {:on-click #(rf/dispatch [::click])
                 :class (c :border [:p 2])}
        "Click Me"]

       [:input {:value @sub :on-change #(rf/dispatch [::change (.. % -target -value)])
                :class (c :border :border-r [:px 2] [:py 1] [:bg :yellow-100] [:ml 5])}]])))

(pages/reg-page ::ctx page)
