(ns anti.util
  (:require
    [stylo.core :refer [c c?]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    #?(:cljs [reagent.core :as r])))

(defn block [{:keys [title width] :or {width "12rem"}} & children]
  [:div
   [:h3 {:class (c [:text :gray-600] :font-hairline :uppercase :text-2xl [:mb 2])} title]
   (into [:div {:style {:display               "grid"
                        :grid-gap              "1rem"
                        :grid-template-columns (str "repeat(auto-fill, minmax(" width ", 1fr))")}}]
         (->> children
              (map (fn [child]
                     [:div {:class (c :border :rounded :flex :items-center :justify-center
                                      [:px 4] [:py 10])}
                      child]))))])

(defn class-names
  "Source: https://github.com/reagent-project/reagent/blob/e70c52531341bba83636e88eb7b60ff5796195b1/src/reagent/impl/util.cljs#L126"
  ([])
  ([class]
   (if (coll? class)
     (let [classes (keep (fn [c]
                           (if c
                             (if (or (keyword? c) (symbol? c))
                               (name c)
                               c)))
                         class)]
       (if (seq classes)
         (str/join " " classes)))
     (if (or (keyword? class) (symbol? class))
       (name class)
       class)))
  ([a b]
   (if a
     (if b
       (str (class-names a) " " (class-names b))
       (class-names a))
     (class-names b)))
  ([a b & rst]
   (reduce class-names
           (class-names a b)
           rst)))

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [then else]
  (if (cljs-env? &env) then else))

(defmacro with-let
  [bindings & body]
  (if-cljs
    `(rf/with-let ~bindings ~@body)
    `(let ~bindings ~@body)))


(defn ratom
  [x]
  #?(:clj  (atom x)
     :cljs (reagent.ratom/atom x)))


(defn cursor
  [src path]
  #?(:clj  (atom (get-in src path))
     :cljs (reagent.ratom/cursor src path)))


(defn track
  [f & args]
  #?(:clj  (apply f args)
     :cljs (apply reagent.ratom/track f args)))


(defn after-render
  [f]
  #?(:clj  (f)
     :cljs (r/after-render f)))


(defn parse-int [s]
  (when s
    #?(:clj (try (Integer/parseInt s) (catch Exception e nil))
       :cljs (try (js/parseInt s) (catch :default e nil)))))


(defn parse-float [s]
  (when s
    #?(:clj (try (Double/parseDouble s) (catch Exception e nil))
       :cljs (try (js/parseFloat s) (catch :default e nil)))))


(defn current-timestamp
  []
  #?(:clj (.getTime (java.util.Date.))
     :cljs (.getTime (js/Date.))))
