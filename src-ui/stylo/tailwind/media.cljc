(ns stylo.tailwind.media
  (:require
    [garden.stylesheet :refer [at-media]]
    [stylo.rule :refer [rule join-rules]]))


;; https://github.com/noprompt/garden/wiki/Media-Queries
(defmethod rule :media
  [_ k & rules]
  [[:& {} (apply at-media
                 (if (keyword? k) {k true} k)
                 (join-rules rules))]])
