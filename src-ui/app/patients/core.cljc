(ns app.patients.core
  (:require
    [zframes.re-frame :as zrf]
    [anti.button]
    [app.routes :refer [href]]
    [stylo.core :refer [c]]
    [app.pages :as pages]))

(zrf/defx ctx
  [_ [_ phase params]]
  (cond (= :init phase) {:http/fetch {:uri  "/Patient"
                                  :unbundle true
                                  :params   {:_count 100}
                                  :path     [::patients]}}))


(zrf/defs model
  [db _]
  (->> (get-in db [::patients :data])
       (mapv (fn [pt]
               (assoc pt :href (href "patients" (:id pt)))))))

(zrf/defview grid
  [model]
  [:div {:class (c :divide-y)}
   (for [pt model]
     [:div {:key (:id pt)
            :class (c :flex :flex-col [:py 1])}
      [:a {:class (c :font-bold) :href (:href pt)}
       (pr-str pt)]])])

(defn page []
  [:div
   [:div {:class (c [:py 8] :w-max-full [:w 200] :mx-auto [:space-y 2])}
    [:div {:class (c :flex)}
     [:a {:href (href "users" "new") :class anti.button/primary-class} "New Patient"]]
    [grid]]])

(pages/reg-page ctx page)
