(ns mappers.chu
  (:refer-clojure :exclude [import]))

(defn operand [ex]
  (fn [e]
    (if (coll? ex)
      (get-in e ex)
      (if (keyword? ex)
        (get e ex)
        ex))))

(def cmp {:= = :< < :> > :<= <= :>= >=})

(defn pred [[op l r]]
  #((get cmp op)
    ((operand l) %) ((operand r) %)))

(defn comp-expr [expr]
  (fn [coll]
    (filterv (pred expr) coll)))

(defn and-expr [[op & exprs]]
  (fn [coll]
    (reduce #(%2 %1)
            coll
            (map comp-expr exprs) )))

(defn or-expr [[op & exprs]]
  (fn [coll]
    (reduce #(concat %1 (%2 coll))
            []
            (map comp-expr exprs) )))

(defn not-expr [[op l]]
  (fn [coll]
    (filter #(not ((operand l) %)) coll)))

(defn contains-predicate [[_ l r]]
  #(contains? (set r) ((operand l) %)))

(defn in-expr [args]
  (fn [coll]
    (filter (contains-predicate args) coll)))

(defn not-in-expr [args]
  (fn [coll]
    (remove (contains-predicate args) coll)))

(defn query [m q]
  (case (first q)
    :and    ((and-expr q) m)
    :or     ((or-expr q) m)
    :not    ((not-expr q) m)
    :in     ((in-expr q) m)
    :not-in ((not-in-expr q) m)
    ((comp-expr q) m)))

(defn indexof [x coll]
  (first (keep-indexed #(when (= %2 x) %1) coll)))

(defn find-idx [m expr]
  (indexof (first (query m expr)) m))

(declare import)
(declare export)


(defn getin [m path]
  (reduce
   (fn [acc p]
     (if (map? p)
       (if (:map p)
         (if (vector? acc)
           (mapv #(export % (:map p)) acc)
           (export acc (:map p)))
         (let [res (vec (query acc (:get p) ))]
           (if (empty? res)
             (:set p)
             res)))
       (if (and (= p :#) (vector? acc))
         acc
         (if (and (vector? acc) (keyword? p))
           (mapv p acc)
           (get acc p)))))
   m path))

(defn deep-merge
  "efficient deep merge"
  [a b]
  (loop [[[k v :as i] & ks] b
         acc a]
    (if (nil? i)
      acc
      (let [av (get a k)]
        (if (= v av)
          (recur ks acc)
          (recur ks (if (and (map? v) (map? av))
                      (assoc acc k (deep-merge av v))
                      (assoc acc k v))))))))

(defn setin [m [k & ks :as path] value]
  (let [v (if ks
            (if (and (= :# (first ks)) (vector? value))
              (vec (map (fn [x] (setin nil (rest ks) x)) value))
              (if (map? k)
                (cond
                  (:get k) (setin (query m (:get k)) ks value)
                  :else nil)
                (setin (get m k) ks value)))
            (if-let [submap (:map k)] 
              (import value submap)
              value))]
    (cond
      (integer? k)
      (assoc (or m (vec (repeat k nil))) k v)

      (map? k)
      (let [set (:set k)
            v   (if (vector? v)
                  (mapv (fn [x] (deep-merge set x)) v)
                  (deep-merge set v))
            should-collection (:get k)
            idx (when-let [getter (:get k)]
                  (find-idx m getter))]
        (if should-collection
          (if idx
            (if (vector? v)
              (assoc m idx  (first v))
              (assoc m idx  v))
            (if (vector? v)
              (vec (concat (or m []) v))
              (conj (or m []) v)))
          v))

      (every? vector? [m v])
      (vec (concat m v))

      (every? nil? [m k])
      v

      (and (vector? m) (= :# k))
      (conj (or m []) v)

      :else
      (assoc m k v)
      )))

(defn import
  [d m]
  (reduce
   (fn [acc [t f]]
     (let [data (getin d f)]
       (if (some? data)
         (setin acc t data)
         acc)))
   {} m))

(defn export
  [d m & [default]]
  (reduce
   (fn [acc [t f]]
     (let [data (getin d t)]
       (if (some? data)
         (setin acc f data)
         acc)))
   (or default {}) m))
