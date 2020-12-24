(ns app.scenario.editor
  (:require [reagent.core :as r]
            #?(:cljs [reagent.dom :as dom])
            [stylo.core :refer [c]]
            #?(:cljs [cljs.pprint :as pp :refer [pprint]])
            [zframes.re-frame :as zrf]
            [re-frame.core :as rf]
            [edamame.core :as edn]
            [clojure.string :as str])
  )


(zrf/defx change-value
  [{db :db} [_ path value]]
  (let [old-value (get-in db path)
        edn-value (cljs.reader/read-string value)
        db* (cond-> db
              (not= old-value edn-value)
              (assoc-in path edn-value))]
   {:db db*}))

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

(defn get-error-marker [{:keys [row end-row col end-col] :as meta} error]
  {:startLineNumber row
   :endLineNumber end-row
   :startColumn col
   :endColumn end-col
   :message (str "Expected " (:expected error) ", but " (:but error))})

;; (defn get-error-models [text errors]
;;   (let [edn-meta (read-edn-with-meta text)
;;         markers (map #(-> edn-meta
;;                           (get-in (:path %))
;;                           meta
;;                           (get-error-marker %)) errors)
;;         decorations (map #(-> edn-meta
;;                               (get-in (:path %))
;;                               meta
;;                               (get-error-decoration %)) errors)]
;;     {:markers markers :decorations decorations}
;;     ))

;; (defn set-model-markers [^js/monaco.editor.ICodeeditor editor value errors]
;;   (let [error-models (get-error-models value errors)
;;         {:keys [markers decorations]} error-models
;;         text-model (.getModel editor)]
;;     (.deltaDecorations editor [] (clj->js decorations))
;;     ))

(defn on-change-value [monaco path]
  (let [value (.getValue monaco)]
    (zrf/dispatch [::change-value path value])))

(zrf/defs ed-sub-dynamic
  [db [_ path]]
  (get-in db path))


(defn zf-editor-inner []
  (let []
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [el (dom/dom-node this)
              {:keys [path errors text read-only]} (r/props this)
              cm (js/CodeMirror.
                  el
                  #js {:lineNumbers true
                       :readOnly read-only
                       :mode "clojure"
                       :value text
                       :lineWrapping true
                       :theme "neo"
                       })]                                        
          (.setSize cm "100%" "100%")
          (.on cm "change"
               (fn []
                 (if-let [edn-value (cljs.reader/read-string (.getValue cm))]
                   (rf/dispatch [::change-value path edn-value]))))))
      :component-did-update
      (fn [this old-argv]
        
        )
      :reagent-render
      (fn []
        [:div {:class (c :h-auto)}])
      }
  )))

(defn zf-editor [path errors read-only]
  (let [data (rf/subscribe [::ed-sub-dynamic path])]
    (fn []
      (let [text (-> @data
                       (pp/write :pretty true :right-margin 60)
                       with-out-str
                       (str/replace "\\n" "\n"))]
        [zf-editor-inner {:text text :path path :errors errors :read-only read-only}])
      )
    )
  )

(comment
  (def errors [{:path [:status] :but 308 :expected 200}])
  (defrecord Wrapper [obj loc])

  (meta (get-in (edn/parse-string "{:body {:status 200}}") [:body :status]))
  (meta (get-in raw-edn [:body :status]))
  (clojure.pprint/pprint raw-edn)
  
  (def path [:body :status]))
