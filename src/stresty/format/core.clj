(ns stresty.format.core
  (:require [cheshire.core]
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
              "ndjson"   'sty/ndjson-fmt})

(defn stderr [& args]
  (let [^String s (str/join " " args)]
    (.println ^java.io.PrintWriter *err* s)))

(defn set-formatter [ztx fmt-name]
  (swap! ztx assoc :formatter 
         (if (nil? fmt-name)
           {'sty/default-fmt (atom {})}
           (if-let [fmt (get formats fmt-name)]
             {fmt (atom {})}
             (do
               (stderr "Unknown format: " fmt-name)
               {'sty/default-fmt (atom {})})))))

(defn emit [ztx event]
  (let [ev (assoc event :ts (System/currentTimeMillis))]
    (when-let [[fmt state] (first (get @ztx :formatter))]
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
