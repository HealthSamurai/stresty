(ns ^:dev/once  app.dev
  (:require [app.core :as core]
            [re-frisk.core :as re-frisk]
            [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(re-frisk/enable)

(defn ^:dev/after-load re-render []
  (println "Reload")
  (core/mount-root))

(core/init!)
