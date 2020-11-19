(ns template
  (:require [clojure.string :as s]))

(defn render [conf template]
  (s/replace
    template
    #"\{([a-zA-Z0-9-_\.]+)\}"
    #(let [template-segments (-> % second (s/split #"\."))]
       (->> template-segments
            (mapv keyword)
            (get-in conf)
            str))))
