(ns mappers.simple
  (:require [clojure.string :as str]))

(defn match [pattern obj]
  (or (= obj pattern)
      (when (and (map? pattern) (map? obj))
        (loop [[[k pv] & kvs] pattern]
          (let [iv (get obj k)]
            (cond
              (nil? k) true
              (nil? iv) false
              (or (= pv iv)
                  (and (map? iv) (map? pv) (match  pv iv)))
              (recur kvs)

              (and (sequential? pv) (sequential? iv)
                   (->> pv
                        (every? (fn [pv'] (->> iv (some #(or (= pv' %) (match pv' %))))))))
              (recur kvs)

              :else false))))))


(defn put-in [obj [p & ps] v]
  (cond
    (nil? p)
    v

    (and (map? p) (empty? ps) (:$fmt p))
    ((:$fmt p) v)

    (and (map? p) (empty? ps) (:$merge p))
    (conj (or obj []) (merge (dissoc p :$merge) v))

    (and (map? p) (empty? ps) (:$conj p))
    (into (or obj [])
          (mapv (fn [v'] (merge (dissoc p :$conj) v')) v))

    (and (map? p) (empty? ps) (:$get p))
    (let [[src dest] (:$get p)
          v' (get v (keyword src))]
      (println "V" v)
      (conj (or obj []) (assoc (dissoc p :$get) (keyword dest) v')))

    (and (map? p) (empty? ps) (:$get-conj p))
    (let [[src dest] (:$get-conj p) ]
      (into (or obj [])
            (->> v
                 (mapv (fn [v']
                         (let [v'' (get v' (keyword src))]
                           (assoc (dissoc p :$get-conj) (keyword dest) v'')))))))

    (or (keyword? p) (string? p))
    (let [p (keyword p)]
      (cond
        (map? obj)
        (assoc obj p (put-in (get obj p) ps v))

        (nil? obj)
        (assoc obj p (put-in nil ps v))
        :else
        (assert false (pr-str obj p ps v))))

    (int? p)
    (cond
      (vector? obj)
      (update obj p (fn [old] (put-in old ps v)) )

      (or (nil? obj) (= {} obj))
      [(put-in nil ps v)]

      ;; (and (= {} obj) (nil? ps))
      ;; v

      :else
      (assert false (pr-str obj (into [p] ps) v)))


    (map? p)
    (if (:$single p)
      (put-in (dissoc p :$single) ps v)
      (cond
        (nil? obj)
        [(put-in p ps v)]

        (vector? obj)
        (loop [[o & os] obj
               res []]
          (if (nil? o)
            (into res [(put-in p ps v)])
            (if (match p o)
              (into (into res [(put-in o ps v)]) os)
              (recur os (conj res o)))))

        :else (assert false (pr-str obj (into [p] ps) v))))



    :else (assert false (pr-str obj (into [p] ps) v))))



(defn apply-mapping [mapping m & [opts]]
  (reduce (fn [acc [k v]]
            ;; (println acc)
            ;; (println k v)
            (if (or (nil? v)
                    (and (string? v) (str/blank? v))
                    (and (string? v) (:nulls opts) (contains? (:nulls opts) (str/trim v))))
              acc
              (if-let [pth (get mapping k)]
                (cond
                  (fn? pth)
                  (pth acc v)

                  (= pth :ignore)
                  acc

                  :else
                  (try
                    (if (set? pth)
                      (->> pth
                           (reduce (fn [acc pth'] (put-in acc pth' v)) acc))
                      (put-in acc pth v))
                    (catch #?(:clj Exception :cljs js/Object) ex
                      (println "Failed: put-in" acc pth v)
                      (throw ex))))
                (do
                  (println "WARN: missed field" (pr-str k ":" v))
                  acc)))
            ) {} m))
