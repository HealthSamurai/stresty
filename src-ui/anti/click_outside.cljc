(ns anti.click-outside
  (:require
    [clojure.string]
    #?(:cljs ["react-outside-click-handler/esm/OutsideClickHandler" :default OutsideClickHandler])))

(defn click-outside [{:keys [on-click]} & children]
  #?(:cljs
     (into [:> OutsideClickHandler {:on-outside-click on-click}]
           children)))
