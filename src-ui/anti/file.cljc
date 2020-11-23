(ns anti.file
  (:require
    [anti.util :refer [class-names block]]
    [anti.button]
    [stylo.core :refer [c c?]]
    #?(:cljs [reagent.core :as r])))


(defn read-file
  [file cb]
  #?(:cljs
     (do
       (doto (new js/FileReader)
         (aset "onload" #(cb (.. % -target -result)))
         (.readAsDataURL file)))))


(defn photo
  [props]
  (let [ref         (volatile! nil)
        src #?(:clj (atom nil) :cljs (r/atom nil))]

    (when (:value props)
      (if (string? (:value props))
        (reset! src (:value props))
        (read-file (:value props) #(reset! src %))))

    (fn [props]
      [:div {:class [(c :flex [:w 30] [:h 30]) (class-names (:class props))]}
       [:button {:class    [anti.button/base-class (c :flex :flex-1 [:p 0] [:text :gray-300] :overflow-hidden)]
                 :on-click #(.click @ref)}
        [:input (merge (dissoc props :class :value :icon-class)
                       {:type      "file"
                        :accept    "image/*"
                        :ref       #(vreset! ref %)
                        :style     {:display "none"}
                        :on-change (fn [e]
                                     (let [file (first (.. e -target -files))]
                                       (set! (.. e -target -value) "")
                                       (read-file file #(reset! src %))
                                       (when-let [on-change (:on-change props)]
                                         (on-change file))))})]
        (if-let [s @src]
          [:img {:src s :class (c :object-cover :w-full :h-full)}]
          [:i.fal.fa-camera {:class [(c [:text-4xl] :m-auto :font-thin)
                                     (class-names (:icon-class props))]}])]])))


(defn demo
  []
  [block {:title "File"}
   [photo {:on-change #(prn "File selected:" %)}]
   [photo {:class (c [:w 20] [:h 20]) :icon-class [(c :text-3xl) :fa-image] :on-change #(prn "File selected:" %)}]
   [photo {:class (c [:w 10] [:h 10]) :icon-class [(c :text-base) :fa-camera-retro] :on-change #(prn "File selected:" %)}]
   [photo {:value "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=400&q=80"}]
   [photo {:class (c [:w 20] [:h 20]) :value "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=400&q=80"}]
   [photo {:class (c [:w 10] [:h 10]) :value "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=400&q=80"}]])
