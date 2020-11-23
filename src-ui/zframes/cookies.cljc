(ns zframes.cookies
  (:require
    #?(:cljs [goog.net.cookies])
    #?(:cljs [cljs.reader :as reader])
    [zframes.re-frame :as zrf]))


#?(:clj (def cookie-storage (atom {})))


(zrf/reg-cofx
  :cookie/get
  (fn [coeffects key]
    (assoc-in coeffects [:cookie key]
              #?(:clj (get @cookie-storage key)
                 :cljs (some-> (.get goog.net.cookies (name key))
                               (reader/read-string))))))


(zrf/defe :cookie/set
  [{k :key v :value}]
  #?(:clj (swap! cookie-storage assoc k v)
     :cljs (.set goog.net.cookies (name k) (pr-str v))))


(zrf/defe :cookie/remove
  [k]
  #?(:clj (swap! cookie-storage dissoc k)
     :cljs (.remove goog.net.cookies (name k) "/")))
