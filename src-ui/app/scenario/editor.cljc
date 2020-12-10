(ns app.scenario.editor
  (:require [reagent.core :as r]
            #?(:cljs [reagent.dom :as dom])
            [stylo.core :refer [c]]
            #?(:cljs [monaco])
            #?(:cljs [cljs.pprint :as pp :refer [pprint]])
            [zframes.re-frame :as zrf]
            [re-frame.core :as rf]
            [edamame.core :as edn]
            [clojure.string :as str])
  )


(zrf/defx change-value
  [{db :db} [_ path value]]
  (prn (cljs.reader/read-string value))
  {:db (assoc-in db path (cljs.reader/read-string value))})

(defn iobj? [x]
  #?(:clj (instance? clojure.lang.IObj x)
     :cljs (satisfies? IWithMeta x)))

(defn read-edn-with-meta [edn]
  (edn/parse-string edn
                    {:postprocess
                     (fn [{:keys [:obj :loc]}]
                       (cond
                         (keyword? obj)
                         obj
                         (iobj? obj)
                         (vary-meta obj merge loc)
                         :else
                         (vary-meta (symbol (str obj)) merge loc)))}))

(defn get-model-markers [text errors]
  (let [edn-meta (read-edn-with-meta text)]
    (map (fn [e]
           (let [meta-path (meta (get-in edn-meta (:path e)))]
             (prn "Error path: " (:path e))
             (prn "Meta path: " meta-path)
             {:startLineNumber (:row meta-path)
              :endLineNumber (:end-row meta-path)
              :startColumn (:col meta-path)
              :endColumn (:end-col meta-path)
              :severity monaco/MarkerSeverity.Error
              :message (str "Expected " (:expected e) ", but " (:but e))}
             ))
         errors)))

(defn set-model-markers [^js/monaco.editor.ICodeeditor editor value errors]
  (let [markers (clj->js (get-model-markers value errors))
        _ (prn "Markers: " markers)
        text-model (.getModel editor)]
    (set! (.-text-model js/window) text-model)
    (set! (.-markers js/window) markers)
    (monaco/editor.setModelMarkers text-model
                                   "jslint"
                                   markers)))

(defn on-change-value [monaco path]
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
  (get-in db path))


(defn zf-editor [path errors]
  (let [data @(rf/subscribe [::ed-sub-dynamic path])
        ]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [el (dom/dom-node this)
              monaco (monaco/editor.create el
                                           (clj->js {:tabSize 2
                                                     :language "clojure"
                                                     :scrollBeyondLastLine false
                                                     :minimap {:enabled false}}))
              text (-> data
                       (pp/write :pretty true :right-margin 60)
                       with-out-str
                       (str/replace "\\n" "\n"))
              _ (.setValue monaco text)
              on-change-editor ((.-onDidChangeModelDecorations monaco) #(update-height monaco el))
              on-change ((.-onDidChangeModelContent monaco) #(on-change-value monaco path))
              ]
          (if errors
            (set-model-markers monaco text errors)
            )
          ))
      :reagent-render
      (fn [stresty-case]
        [:div {:class (c [:h 100])}])
      }
     ))
  )

(comment
  (def errors [{:path [:status] :but 308 :expected 200}])
  (defrecord Wrapper [obj loc])

  (meta (get-in (edn/parse-string "{:body {:status 200}}") [:body :status]))
  (meta (get-in raw-edn [:body :status]))
  (clojure.pprint/pprint raw-edn)

  

  
  (def path [:body :status])
  
 
  
  
  
  )
