(ns stresty.sci
  (:require [sci.core :as sci]))


(defn sci-get-in [state path]
  (get-in state path))

(def opts {:namespaces {'scy {'get-in sci-get-in}}})

(defn eval-form [opts expr]
  (sci/eval-form (sci/init opts) expr))

(defn *eval-data [ctx data]
  (cond
    (list? data)
    (sci/eval-form ctx data)

    (map? data)
    (->> data
         (reduce (fn [acc [k v]]
                   (assoc acc k (*eval-data ctx v))
                   ) {}))

    (sequential? data)
    (->> data
         (mapv (fn [x] (*eval-data ctx x))))

    :else data))

(defn eval-data [opts expr]
  (let [res (*eval-data (sci/init opts) expr)]
    ;; (println ::eval expr)
    ;; (println ::res res)
    res))

