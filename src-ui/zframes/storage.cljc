(ns zframes.storage
  (:require
    [zframes.re-frame :as zrf]))


#?(:clj (def local-storage (atom {})))


(zrf/reg-cofx
  :storage/get
  (fn [coeffects k]
    (assoc-in coeffects [:storage k]
              #?(:clj (get @local-storage k)
                 :cljs (try
                         (some-> (js/window.localStorage.getItem (str k))
                                 (js/decodeURIComponent)
                                 (js/JSON.parse)
                                 (js->clj :keywordize-keys true))
                         (catch :default _
                           nil))))))


(zrf/defe :storage/set
  [items]
  (doseq [[k v] items]
    #?(:clj (swap! local-storage assoc k v)
       :cljs (js/window.localStorage.setItem
               (str k)
               (-> (clj->js v)
                   (js/JSON.stringify)
                   (js/encodeURIComponent))))))


(zrf/defe :storage/remove
  [keys]
  (doseq [k keys]
    #?(:clj (swap! local-storage dissoc k)
       :cljs (js/window.localStorage.removeItem (str key)))))
