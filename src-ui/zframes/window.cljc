(ns zframes.window
  (:require
    [zframes.re-frame :as zrf]))


(zrf/defe :window/before-unload
  [message]
  #?(:cljs
     (set! (.-onbeforeunload js/window)
           (when message (constantly message)))))


(zrf/defe :window/add-event-listener
  [args]
  #?(:cljs
     (js/addEventListener.apply nil (clj->js args))))


(zrf/defe :window/remove-event-listener
  [args]
  #?(:cljs
     (js/removeEventListener.apply nil (clj->js args))))


(zrf/defe :window/scroll-to
  [args]
  #?(:cljs
     (js/scrollTo.apply nil (clj->js args))))


(zrf/defe :window/print
  [_]
  #?(:cljs
     (js/print)))
