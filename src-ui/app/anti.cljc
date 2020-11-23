(ns app.anti
  (:require [app.pages :as pages]
            [anti.tags-list]
            [anti.date-input]
            [anti.alt-dropdown]
            [anti.button]
            [anti.spin]
            [anti.input]
            [anti.native-select]
            [anti.checkbox]
            [anti.alt-select]
            [anti.simple-radio]
            [anti.calendar]
            [anti.file]
            [stylo.core :refer [c]]
            [zframes.re-frame :as zrf]))


(zrf/reg-event-fx
  index
  (fn [_ _]))


(defn page []
  [:div {:class (c [:p 6] [:space-y 8])}
   [anti.date-input/demo]
   [anti.alt-select/demo]
   [anti.file/demo]
   [anti.button/demo]
   [anti.spin/demo]
   [anti.tags-list/demo]
   [anti.alt-dropdown/demo]
   [anti.input/demo]
   [anti.native-select/demo]
   [anti.checkbox/demo]
   [anti.simple-radio/demo]
   [anti.calendar/demo]])


(pages/reg-page index page)
