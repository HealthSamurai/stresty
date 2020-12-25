(ns app.hack.codemirror
  (:require [clojure.string :as str]
            [chrono.core :as chrono]
            [chrono.now]
            [anti.date-input]
            [reagent.core :as r]
            [anti.button]
            [app.yaml]
            #?(:cljs [reagent.dom :as dom])
            [re-frame.core :as rf]))

(defn codemirror
  ([node]
   #?(:cljs (js/CodeMirror. node)))
  ([node config]
   #?(:cljs (js/CodeMirror. node (clj->js config)))))

(def default-cm-options
  {:lineNumbers true
   :height "auto"
   :mode "yaml"
   :lint true
   :matchBrackets true
   :viewportMargin #?(:cljs js/Infinity :clj Integer/MAX_VALUE)})



(defn input [path & [attrs]]
  (let [value (rf/subscribe [:app.hack.core/get-value path])
        cm (atom nil)
        st (atom attrs)
        cm-opts (merge default-cm-options attrs)
        ;; attrs (assoc attrs :on-change #(rf/dispatch [:zf/set-value form-path path (.. % -target -value)]))
        ]
    #?(:cljs
       (r/create-class
              {:reagent-render (fn [path & [attrs]]
                                 (reset! st attrs)
                                 @value ;; This code is needed to update textarea value on db update
                                 [:div.zen-codemirror])

               :component-did-mount
               (fn [this]
                 (let [*cm (codemirror (dom/dom-node this) cm-opts)
                       sv (aget *cm "setValue")
                       gv (aget *cm "getValue")
                       on (aget *cm "on")]
                   (reset! cm *cm)
                   (.call sv *cm (.toString (or @value (get cm-opts "value") "")))
                   ;; (.setValue *cm (.toString (or @value "")))
                   (.call on *cm "change"
                          (fn [& _] (rf/dispatch [:app.hack.core/set-value path (.call gv *cm)])))))

               :component-did-update
               (fn [this [_ old-props]]
                 (let [*cm @cm
                       vvalue (or @value "")]

                   (if-not (= vvalue (.call (aget *cm "getValue") *cm))
                     (.setOption ^js/CodeMirror *cm "value" vvalue))

                   (doseq [[k v] @st]
                     (if (not= k "extraKeys")
                       (.setOption ^js/CodeMirror *cm k v)))))}))))
