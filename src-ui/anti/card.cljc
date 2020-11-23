(ns anti.card
  (:require
    [chrono.calendar :as cal]
    [stylo.core :refer [c c?]]
    [anti.spin]
    [anti.util :refer [class-names block]]))


(defn card
  [props & children]
  [:div {:class [(c :border :rounded :w-full) (class-names (:class props))]}
   (when (or (:title props) (:extra props))
     [:div {:class (c :flex :items-center [:px 6] [:py 3] :border-b)}
      (when (:title props)
        [:div {:class (c :text-lg :font-medium)} (:title props)])
      (when (:extra props)
        [:div {:class (c :ml-auto)}
         (:extra props)])])
   (into [:div {:class [(c [:px 6] [:py 4]) (class-names (:body-class props))]}] children)])


(defn demo
  []
  [block {:title "Card" :width "16rem"}
   [card {} "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco"]
   [card {:title "Titled card"} "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco"]
   [card {:title "Card with extra" :extra [:div {:class (c [:text :pink-500])} "I am extra"]} "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco"]])
