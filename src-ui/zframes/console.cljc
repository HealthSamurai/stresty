(ns zframes.console
  (:require
    [zframes.re-frame :as zrf]))

(zrf/defe :console/log
  [args]
  #?(:cljs (apply js/console.log args)))

(zrf/defe :console/warn
  [args]
  #?(:cljs (apply js/console.warn args)))

(zrf/defe :console/error
  [args]
  #?(:cljs (apply js/console.error args)))
