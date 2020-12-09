(ns app.routes
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [route-map.core :as route-map]))

(def global-contexts {})

(def routes {"anti"          {:. :app.anti/index}
             "reagent"  {:. :app.reagent/ctx}
             "reframe" {:. :app.reframe/ctx}
             "case" {:. :app.case.core/ctx}
             "scenario" {:. :app.scenario.core/index
                         [:ns] {[:name] {:. :app.scenario.show/index}}}
             "config" {:. :app.config.core/index}
             })

(defn to-query-params [params]
  (->> params
       (map (fn [[k v]] (str (name k) "=" v)))
       (str/join "&")))

(defn href [& parts]
  (let [params (if (map? (last parts)) (last parts) nil)
        parts (if params (butlast parts) parts)
        url (str "/" (str/join "/" (map (fn [x] (if (keyword? x) (name x) (str x))) parts)))]

    (when-not  (route-map/match [:. url] routes)
      (println (str url " is not matches routes")))
    (str "#" url (when params (str "?" (to-query-params params))))))

(defn back [fallback]
  {:href (or @(rf/subscribe [:pop-route]) fallback)})
