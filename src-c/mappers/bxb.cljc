(ns mappers.bxb
  (:require [mappers.match :as match]))

(declare *get)
(declare *put)

(defmulti bxb (fn [k & opts] k))

(defn match? [obj pat]
  (match/match obj pat))

(defmethod bxb
  :map-by
  [_ pat dir obj path & [val]]
  (cond
    ;; todo check uniq
    (= dir :get) (when (sequential? obj) (first (filter #(match? % pat) obj)))
    (= dir :put) (cond
                   (or (nil? obj) (and (sequential? obj) (empty? obj))) [(*put pat path val)]
                   (sequential? obj) (loop [[o & os] obj
                                            res []]
                                       (if (match? o pat)
                                         (into res (into [(*put o path val)] os))
                                         (if (empty? os)
                                           (conj res o (*put pat path val))
                                           (recur os (conj res o)))))
                   :else (assert false (pr-str [dir obj pat])))
    :else (assert false (pr-str [dir obj pat]))))

(defn *get [obj pth & [{pn? :preserve-nil?}]]
  (if (or (empty? pth) (nil? obj))
    obj
    (loop [[p & ps] pth
           obj obj]
      (let [obj' (cond (and (or (string? p) (keyword? p)) (map? obj)) (let [v (get obj p)]
                                                                        (if (and (nil? v) pn? (contains? obj p))
                                                                          ::nil
                                                                          (get obj p)))
                       (and (integer? p) (sequential? obj)) (nth obj p nil)
                       (vector? p) (apply bxb (into p [:get obj ps]))
                       :else (assert false (pr-str ["current path" p "rest path" ps "from" obj])))]
        (if (empty? ps)
          obj'
          (if (nil? obj')
            nil
            (recur ps obj')))))))

(defn *put [obj [p & ps] v]
  (cond
    (nil? p)  v

    (or (keyword? p) (string? p))
    (let [p (keyword p)]
      (cond
        (map? obj)
        (assoc obj p (*put (get obj p) ps v))

        (nil? obj)
        (assoc obj p (*put nil ps v))

        :else
        (assert false (pr-str obj p ps v))))

    (int? p)
    (cond
      (vector? obj)
      (update obj p (fn [old] (*put old ps v)))

      (or (nil? obj) (= {} obj))
      [(*put nil ps v)]

      :else
      (assert false (pr-str obj (into [p] ps) v)))

    (vector? p)
    (apply bxb (into p [:put obj ps v]))

    :else (assert false (pr-str obj (into [p] ps) v))))


(defn xget [dirs src & [default]]
  (->> dirs
       (partition 2)
       (reduce (fn [dest [from to]]
                 (let [v (*get src from)]
                   (if-not (nil? v)
                     (*put dest to v)
                     dest)))
               (or default {}))))

(defn xput [dirs src & [default]]
  (->> dirs
       (partition 2)
       (reduce (fn [dest [to from]]
                 (let [v (*get src from {:preserve-nil? true})]
                   (cond
                     (= ::nil v) (*put dest to nil)
                     (nil? v) dest
                     :else (*put dest to v))))
               (or default {}))))
