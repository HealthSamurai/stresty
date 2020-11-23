(ns anti.button
  (:require
    [anti.util :refer [block]]
    [anti.spin :refer [spin]]
    [stylo.core :refer [c c?]]
    [re-frame.core :as rf]
    [anti.util :refer [class-names]]))

(def base-class
  (c [:px 4] [:py 1]
     :inline-flex
     :items-center
     :cursor-pointer
     [:leading-relaxed]
     :border
     :rounded
     :whitespace-no-wrap
     [:bg :white]
     :transition-all [:duration 200] :ease-in-out
     [:focus :outline-none :shadow-outline]
     [:pseudo ":not(:disabled)"
      [:hover [:text :blue-500] [:border :blue-500]]
      [:active [:text :blue-800] [:border :blue-800]]]
     [:disabled [:text :gray-500] [:bg :gray-200] [:border :gray-400] :cursor-not-allowed]))

(def primary-class
  (c [:bg :blue-500]
     [:border :transparent]
     [:text :white]
     [:pseudo ":not(:disabled)"
      [:hover [:text :white] [:bg :blue-400] [:border :transparent]]
      [:active [:text :white] [:bg :blue-600] [:border :transparent]]]))

(def link-class
  (c [:bg :transparent]
     [:border :transparent]
     [:text :blue-600]
     [:pseudo ":not(:disabled)"
      [:hover [:text :blue-500] [:bg :transparent] [:border :transparent]]
      [:active [:text :blue-800] [:bg :transparent] [:border :transparent]]]
     [:disabled [:text :gray-500] [:bg :transparent] [:border :transparent] :cursor-not-allowed]))

(def text-class
  (c [:bg :transparent]
     [:border :transparent]
     [:pseudo ":not(:disabled)"
      [:hover [:text :inherit] [:bg :gray-100] [:border :transparent]]
      [:active [:text :inherit] [:bg :gray-200] [:border :transparent]]]
     [:disabled [:text :gray-500] [:bg :transparent] [:border :transparent] :cursor-not-allowed]))

(def small-class
  (c [:px 3] [:py 0.25]))

(def group-class
  (c :relative
     [:rounded 0]
     [:first-child :rounded-l]
     [:last-child :rounded-r]
     [:pseudo ":not(:first-child)" [:ml "-1px"]]
     [:focus [:z 2]] [:hover [:z 1]]))


(defn button
  [props & children]
  (into [:button (merge (dissoc props :class :type :loading :size :on-click)
                        {:class    [base-class
                                    (case (:type props)
                                      "primary" primary-class
                                      "link" link-class
                                      "text" text-class
                                      nil)
                                    (case (:size props)
                                      "small" small-class
                                      nil)
                                    (class-names (:class props))]
                         :on-click (when-not (:loading props)
                                     (:on-click props))})]
        (cond-> children
          (:loading props)
          (conj [spin {:class (c [:mr 2])}]))))

(defn zf-button
  [props & children]
  (into [button (merge (dissoc props :on-click)
                       (when-let [e (:on-click props)]
                         {:on-click #(rf/dispatch e)}))]
        children))

(defn demo
  []
  [block {:title "Buttons"}

   [button {} "Default"]
   [button {:size "small"} "Small"]
   [button {:disabled true} "Default Disabled"]
   [button {:loading true} "Default Loading"]

   [button {:type "primary"} "Primary"]
   [button {:type "primary" :disabled true} "Primary Disabled"]
   [button {:type "primary" :loading true} "Primary Loading"]

   [button {:type "text"} "Text"]
   [button {:type "text" :disabled true} "Text Disabled"]
   [button {:type "text" :loading true} "Text Loading"]

   [button {:type "link"} "Link"]
   [button {:type "link" :disabled true} "Link Disabled"]
   [button {:type "link" :loading true} "Link Loading"]

   [:span
    [button {:class group-class} "1"]
    [button {:class group-class} "2"]
    [button {:class group-class} "3"]]

   [button {:class (c :block [:flex-grow 1])} "Block"]
   [zf-button {:on-click [::hello]} "Dispatch"]])
