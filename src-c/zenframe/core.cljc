(ns zenframe.core
  (:require [reagent.core]))

(def app-db
  #?(:clj (atom {})
     :cljs (reagent.core/atom {})))

(defn >>
  "dispatch event (z/>> #'ev {})"
  [ev & args]
  (let [edb (if-let [cursor (:cursor (meta ev))]
              (do 
                (println "cur>>" ev cursor)
                #?(:cljs (reagent.core/cursor app-db cursor)))
              app-db)
        fx {:db @edb}]
    (let [{db :db :as res} (apply ev fx args)]
      (when db (reset! edb db))
      res)))

(defn <<
  "subscribe on sub (z/<< #'sub)"
  [sub & args]
  #?(:cljs
     (let [db (if-let [cursor (:cursor (meta sub))]
                (do
                  (println "cur<<" cursor)
                  (reagent.core/cursor app-db cursor))
                app-db)]
       (reagent.core/track (fn [args] (apply sub @db args)) args))))


#?(:clj
   (defmacro defv
     [name subs body]
     (let [subs (->> subs
                     (partition 2)
                     (map (fn [[b nm]]
                            (let [n (gensym "sub")]
                              [[n (list 'zenframe.core/<< nm)]
                               [b (list 'deref n)]]))))]
       `(defn ~name
          []
          (let [~@(mapcat first subs)]
            (fn []
              (let [~@(mapcat second subs)]
                ~body)))))))
