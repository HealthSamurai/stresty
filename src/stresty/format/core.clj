(ns stresty.format.core
  (:require [zen.core :as zen]
            [cheshire.core]
            [zprint.core :as zprint]
            [stresty.format.compact :as compact]
            [stresty.format.default :as default]
            [stresty.format.detailed :as detailed]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defmulti do-format (fn [ztx fmt state event] fmt))

(def formats {"compact"  'sty/compact-fmt
              "default"  'sty/default-fmt
              "detailed" 'sty/detailed-fmt
              "html"     'sty/html-fmt
              "ndjson"   'sty/ndjson-fmt
              "json"     'sty/json-fmt})

(defn set-formatters [ztx fmt-names]
  (let [fmts (->> fmt-names
                 (mapv (fn [x] (get formats x)))
                 (filter identity)
                 (reduce (fn [acc fmt] (assoc acc fmt (atom {}))) {}))
        fmts (if (empty? fmts) {'sty/default-fmt (atom {})} fmts)]
    (swap! ztx assoc :formatters fmts)))

(defn emit [ztx event]
  (let [ev (assoc event :ts (System/currentTimeMillis))]
    (doseq [[fmt state] (get @ztx :formatters)]

      (do-format ztx fmt state ev))))

(defmethod do-format
  'sty/compact-fmt
  [ztx _ state event]
  (#'compact/do-format ztx state event))

(defmethod do-format
  'sty/default-fmt
  [ztx _ state event]
  (#'default/do-format ztx state event))

(defmethod do-format
  'sty/detailed-fmt
  [ztx _ state event]
  (#'detailed/do-format ztx state event))

(defmethod do-format
  'sty/ndjson-fmt
  [ztx _ state event]
  (println (cheshire.core/generate-string event)))

(comment

  (->>
   (zprint/czprint-str
    {:body {:a 1}
     :header {:xxxx 3}})
   println)

  )
