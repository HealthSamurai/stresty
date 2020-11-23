(ns zframes.dom
  (:require
    [zframes.re-frame :as zrf]))


(zrf/defe :dom/select
  [{:keys [id start end]}]
  #?(:cljs
     (when-let [el (js/document.getElementById id)]
       (set! (.-selectionStart el) start)
       (set! (.-selectionEnd el) end))))


(zrf/defe :dom/focus
  [id]
  #?(:cljs
     (when-let [el (js/document.getElementById id)]
       (.focus el))))


(zrf/defe :dom/blur
  [id]
  #?(:cljs
     (if id
       (some-> (js/document.getElementById id) .blur)
       (some-> js/document.activeElement .blur))))


(zrf/defe :dom/scroll-into
  [id]
  #?(:cljs
     (when-let [el (js/document.getElementById id)]
       (.scrollIntoView el false))))

