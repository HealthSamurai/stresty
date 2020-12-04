(ns app.scenario.editor
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [stylo.core :refer [c]]
            [monaco]
            [zframes.re-frame :as zrf]
            [re-frame.core :as rf])
  )


(zrf/defx change-value
  [{db :db} [_ path value]]
  (prn (cljs.reader/read-string value))
  {:db (assoc-in db path (cljs.reader/read-string value))})

(defn on-change-value [monaco path]
  (let [value (.getValue monaco)]
    (zrf/dispatch [::change-value path value])))

(defn zf-editor [path]
  (let [sub-name (keyword (->> path
                               (mapv str)
                               (clojure.string/join ".")))
        _ (prn sub-name)
        _ (rf/reg-sub sub-name (fn [db _] (get-in db path)))
        data @(rf/subscribe [sub-name])]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [el (dom/dom-node this)
              monaco (monaco/editor.create el
                                           #js {:tabSize 2
                                                :language "clojure"})
              on-change ((.-onDidChangeModelContent monaco) #(on-change-value monaco path))
              ]
          (.setValue monaco (with-out-str (cljs.pprint/pprint data)))
          
          ))
      :reagent-render
      (fn [stresty-case]
        [:div {:class (c [:h 100])}]
        )
      }
     ))
  )

