(ns zframes.re-frame
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [re-frame.interop])
  #?(:cljs
     (:require-macros
      [zframes.re-frame :refer [reg-event-fx reg-sub defview]])))

;; -- dispatch ----------------------------------------------------------------

(def dispatch rf/dispatch)
(def dispatch-sync rf/dispatch-sync)

#?(:clj
   (defmacro reg-sub
     [query-id & args]
     (assert (symbol? query-id)
             (format "Query id must be type of symbol but %s given" (type query-id)))
     (let [k (keyword (str *ns*) (name query-id))]
       `(do (def ~query-id ~k)
            (rf/reg-sub ~k ~@args)))))

#?(:clj
   (defmacro defs
     [query-id & fn-def]
     (assert (symbol? query-id)
             (format "Query id must be type of symbol but %s given" (type query-id)))
     (let [k (keyword (str *ns*) (name query-id))
           fn-name (symbol (str (name query-id) "-sub"))
           [sigs fn-def] (loop [sigs [] [x & xs :as xss] fn-def]
                           (if (nil? x)
                             (assert false (str "Expected fn def, but " (pr-str fn-def)))
                             (if (symbol? x)
                               (let [sigs' (conj sigs :<- [x])]
                                 (recur sigs' xs))
                               [sigs xss])))]
       `(do (def ~query-id ~k)
            (defn ~fn-name ~@fn-def)
            (rf/reg-sub ~k ~@sigs ~fn-name)))))
#?(:clj
   (defmacro defx
     [fx-id & fn-def]
     (assert (symbol? fx-id)
             (format "Query id must be type of symbol but %s given" (type fx-id)))
     (let [k (keyword (str *ns*) (name fx-id))
           fn-name (symbol (str (name fx-id) "-fx"))
           pth? (first fn-def)]
       (if (keyword? pth?)
         (let [[pth & fn-def] fn-def]
           `(do (def ~fx-id ~k)
                (defn ~fn-name  ~@fn-def)
                (rf/reg-event-fx ~k [(rf/path ~(keyword pth))] ~fn-name)))

         `(do (def ~fx-id ~k)
              (defn ~fn-name ~@fn-def)
              (rf/reg-event-fx ~k ~fn-name))))))

#?(:clj
   (defmacro defd
     [fx-id & fn-def]
     (assert (symbol? fx-id)
             (format "Query id must be type of symbol but %s given" (type fx-id)))
     (let [k (keyword (str *ns*) (name fx-id))
           fn-name (symbol (str (name fx-id) "-fx"))
           pth? (first fn-def)]
       (if (keyword? pth?)
         (let [[pth & fn-def] fn-def]
           `(do (def ~fx-id ~k)
                (defn ~fn-name  ~@fn-def)
                (rf/reg-event-db ~k [(rf/path ~(keyword pth))] ~fn-name)))

         `(do (def ~fx-id ~k)
              (defn ~fn-name ~@fn-def)
              (rf/reg-event-db ~k ~fn-name))))))

#?(:clj
   (defmacro defe
     [fx-id fn-args & fn-def]
     (assert (keyword? fx-id)
             (format "Query id must be type of keyword but %s given" (str (type fx-id))))
     (assert (and (vector? fn-args) (= 1 (count fn-args)))
             "Args must be a vector with single element")
     `(do (rf/reg-fx ~fx-id (fn ~fn-args ~@fn-def))
          (rf/reg-event-fx ~fx-id (fn [~'_ [~'_ ~'v]] {~fx-id ~'v})))))


(def subscribe rf/subscribe)
(def clear-sub rf/clear-sub)
(def clear-subscription-cache! rf/clear-subscription-cache!)

#?(:clj
   (defmacro reg-sub-raw
     [query-id handler-fn]
     (assert (symbol? query-id)
             (format "Query id must be type of symbol but % given" (type query-id)))
     (let [k (keyword (str *ns*) (name query-id))]
       `(do (def ~query-id ~k)
            (rf/reg-sub-raw ~k ~handler-fn)))))

;; -- effects -----------------------------------------------------------------

(def reg-fx rf/reg-fx)
(def clear-fx rf/clear-fx)

;; -- coeffects ---------------------------------------------------------------

(def reg-cofx rf/reg-cofx)
(def inject-cofx rf/inject-cofx)
(def clear-cofx rf/clear-cofx)

;; -- events ------------------------------------------------------------------

#?(:clj
   (defmacro reg-event-db
     ([id handler]
      `(reg-event-db ~id nil ~handler))
     ([id interceptors handler]
      (assert (symbol? id)
              (format "Event id must be type of symbol but % given" (type id)))
      (let [k (keyword (str *ns*) (name id))]
        `(do (def ~id ~k)
             (rf/reg-event-db ~k ~interceptors ~handler))))))

#?(:clj
   (defmacro reg-event-fx
     ([id handler]
      `(reg-event-fx ~id nil ~handler))
     ([id interceptors handler]
      (assert (symbol? id)
              (format "Event id must be type of symbol but % given" (type id)))
      (let [k (keyword (str *ns*) (name id))]
        `(do (def ~id ~k)
             (rf/reg-event-fx ~k ~interceptors ~handler))))))

(def reg-event-ctx rf/reg-event-ctx)
(def clear-event rf/clear-event)

;; -- interceptors ------------------------------------------------------------

(def debug rf/debug)
(def enrich rf/enrich)
(def trim-v rf/trim-v)
(def after rf/after)
(def on-changes rf/on-changes)
(def ->interceptor rf/->interceptor)
(def get-coeffect rf/get-coeffect)
(def assoc-coeffect rf/assoc-coeffect)
(def get-effect rf/get-effect)
(def assoc-effect rf/assoc-effect)
(def enqueue rf/enqueue)
(def ratom #?(:cljs r/atom :clj atom))

;; -- components ------------------------------------------------------------

#?(:clj
   (defmacro defview
     [name subs & body]
     (let [subs (->> subs
                     (map (fn [name]
                            (let [n (gensym "sub")]
                              [[n (list 're-frame.core/subscribe [name])]
                               [name (list 'deref n)]]))))]
       `(defn ~name
         []
         (let [~@(mapcat first subs)]
           (fn []
             (let [~@(mapcat second subs)]
               ~@body)))))))

(comment
  (macroexpand-1
    '(defview my-comp
       [name color]
       [:div (str name color)])))

(rf/reg-event-fx
  :dispatch-n
  (fn [_ [_ & events]]
    {:dispatch-n events}))


;; -- subscription cofx -----------------------------------------------------


(rf/reg-cofx :sub (re-frame.registrar/get-handler :cofx :vimsical.re-frame.cofx.inject/sub))


;; -- extended path interceptor ---------------------------------------------

(defn path
  "Works just like standard path interceptor but when fn is given it will be called
  with event to get path vector."
  [maybe-fn & args]
  (let [get-path (if (fn? maybe-fn) maybe-fn (constantly (cons maybe-fn args)))]
    (->interceptor
      :id :path-ex
      :before (fn [context]
                (update context :queue
                        (fn [queue]
                          (-> re-frame.interop/empty-queue
                              (conj (rf/path (get-path (get-coeffect context :event))))
                              (into queue))))))))
