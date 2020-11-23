(ns anti.spin
  (:require
    [anti.util :refer [block]]
    [stylo.core :refer [c c?]]))

(defn spin [props]
  [:i.far.fa-spinner-third.fa-spin props])

(defn demo
  []
  [block {:title "Spins"}
   [spin {}]
   [spin {:class (c :text-lg)}]
   [spin {:class (c :text-2xl)}]])
