(ns app.scenario.editor
  (:require [reagent.core :as r]
            #?(:cljs [reagent.dom :as dom])
            [stylo.core :refer [c]]
            #?(:cljs [monaco])
            #?(:cljs [cljs.pprint :refer [pprint]])
            [zframes.re-frame :as zrf]
            [re-frame.core :as rf])
  )


(zrf/defx change-value
  [{db :db} [_ path value]]
  (prn (cljs.reader/read-string value))
  {:db (assoc-in db path (cljs.reader/read-string value))})

(defn set-model-markers [editor]
  #_(let [markers [(clj->js {:startLineNumber 1
                           :endLineNumber 2
                           :startColumn 1
                           :endColumn 20
                           :message "Expected 200, but 201"
                           :severity monaco/MarkerSeverity.Error})]
        text-model (.getModel editor)]
    
    (monaco/editor.setModelMarkers text-model
                                   "jslint"
                                   markers))
  )

(defn on-change-value [monaco path]
  (prn "On-change-value")
  (set-model-markers monaco)
  (let [value (.getValue monaco)]
    (zrf/dispatch [::change-value path value])))

(defn update-height [^js/monaco.editor.ICodeEditor editor dom-el]
  (let [line-height (.getOption editor (.-lineHeight (.-EditorOption (.-editor monaco))))
        line-count (.getLineCount (.getModel editor))
        height (+ (.getTopForLineNumber editor (+ line-count 1)) line-height)]
    (set! (.-height (.-style dom-el)) (str height "px"))
    (.layout editor)
    ))

(zrf/defs ed-sub-dynamic
  [db [_ path]]
  (prn "Path: " path)
  (prn "Path value: " path)
  (get-in db path))

(defn zf-editor [path]
  (let [data @(rf/subscribe [::ed-sub-dynamic path ])]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [el (dom/dom-node this)
              monaco (monaco/editor.create el
                                           (clj->js {:tabSize 2
                                                     :language "clojure"
                                                     :scrollBeyondLastLine false
                                                     :minimap {:enabled false}}))
              _ (.setValue monaco (with-out-str (pprint data)))
              on-change-editor ((.-onDidChangeModelDecorations monaco) #(update-height monaco el))
              on-change ((.-onDidChangeModelContent monaco) #(on-change-value monaco path))
              ]
          ))
      :component-will-unmount
      (fn [this]
        (prn "Haha unmount component")
        )
      :component-did-update
      (fn [this]
        (prn "Component did update path: " path)
        )
      :reagent-render
      (fn [stresty-case]
        [:div {:class (c [:h 100])}])
      }
     ))
  )

