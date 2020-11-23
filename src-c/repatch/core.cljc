(ns repatch.core
  (:require
    [clojure.set :as set]
    [medley.core :as medley]))


(defn index-by [k coll]
  (reduce
    (fn [acc x]
      (if-some [kv (get x k)]
        (assoc acc kv x)
        acc))
    {} coll))


(defn find-matching
  [pattern xs]
  (->> (map-indexed vector xs)
       (some (fn [[i x]]
               (when (= pattern (select-keys x (keys pattern)))
                 i)))))


(defn into-batch
  [ops]
  (if (empty? ops)
    nil
    (if (= 1 (count ops))
      (first ops)
      (into ["batch"] ops))))


(declare diff)


(defn diff-maps
  [{:keys [schema] :as ctx} old new]
  (reduce-kv
    (fn [acc k new-v]
      (let [old-v (get old k)
            {stp :type :as subs-schema} (get schema k)]
        (cond
          (= old-v new-v)
          acc

          (nil? new-v)
          (assoc acc k ["delete" old-v])

          (nil? old-v)
          (assoc acc k (cond
                         (= stp :key)
                         ["into" new-v] ;; this logic is wrong
                         :else ["change" nil new-v]))

          :else
          (medley/assoc-some
            acc k
            (diff (assoc ctx :schema subs-schema)
                  old-v new-v)))))
    nil new))


(defmulti diff-sequentials
  (fn [ctx _ _]
    (get-in ctx [:schema :type])))


(defn scalar-seq?
  [xs]
  (not (map? (first xs))))


(defmethod diff-sequentials nil
  [{:keys [schema] :as ctx} old new]
  (if (scalar-seq? old)
    ["change" old (filterv #(not (nil? %)) new)]
    (->> (range (max (count old) (count new)))
         (reduce
           (fn [ops i]
             (let [old-v (nth old i nil)
                   new-v (nth new i nil)]
               (cond
                 (= old-v new-v)
                 ops

                 (nil? new-v)
                 (conj ops ["nth" i ["delete" old-v]])

                 (nil? old-v)
                 (conj ops ["nth" i ["change" old-v new-v]])

                 :else
                 (if-let [v' (diff schema old-v new-v)]
                   (conj ops ["nth" i ["patch" v']])
                   ops))))
           [])
         (into-batch))))


(defmethod diff-sequentials :set
  [ctx old new]
  (let [old-s   (set old)
        new-s   (set new)
        to-conj (set/difference new-s old-s)
        to-disj (set/difference old-s new-s)]
    (cond
      (and (seq to-conj) (seq to-disj))
      ["batch"
       (into ["conj"] to-conj)
       (into ["disj"] to-disj)]

      (seq to-conj)
      (into ["conj"] to-conj)

      (seq to-disj)
      (into ["disj"] to-disj))))


(defmethod diff-sequentials :key
  [{:keys [schema]} old new]
  (let [k     (:key schema)
        old-k (index-by k old)
        new-k (index-by k new)]
    (->> (concat (keys old-k) (keys new-k))
         (distinct)
         (reduce
           (fn [ops kv]
             (let [old-v (get old-k kv)
                   new-v (get new-k kv)]
               (cond
                 (= old-v new-v)
                 ops

                 (and (nil? old-v))
                 (conj ops ["conj" new-v])

                 (and (nil? new-v))
                 (conj ops ["find" {k kv} ["delete" old-v]])

                 :else
                 (if-let [v' (diff schema old-v new-v)]
                   (conj ops ["find" {k kv} ["patch" v']])
                   ops))))
           [])
         (into-batch))))


(defn diff
  ([old new]
   (diff nil old new))
  ([ctx old new]
   (cond
     (= old new)
     nil

     (and (map? old) (map? new))
     (diff-maps ctx old new)

     (and (sequential? old) (sequential? new))
     (diff-sequentials ctx old new)

     :else
     new)))


(declare patch)

(comment
  (def run-op nil))


(defmulti run-op
  (fn [target op]
    (first op)))


(defmethod run-op
  "change"
  [target [_ old-v new-v]]
  new-v)


(defmethod run-op
  "patch"
  [target [_ diff]]
  (patch target diff))


(defmethod run-op
  "batch"
  [target [_ & ops]]
  (reduce run-op target ops))


(defmethod run-op
  "conj"
  [target [_ v]]
  {:pre [(or (coll? target) (nil? target))]}
  (if (some #(= v %) target)
    target
    (conj (or target []) v)))

(defmethod run-op
  "into"
  [target [_ v]]
  {:pre [(or (coll? target) (nil? target))]}
  (into (or target []) v))


(defmethod run-op
  "disj"
  [target [_ v]]
  {:pre [(or (nil? target) (coll? target))]}
  (->> (remove #(= v %) target)
       (into (empty target))))


(defmethod run-op
  "nth"
  [target [_ index op]]
  {:pre [(sequential? target)
         (<= 0 index (count target))]}
  (let [[h [x & t]] (split-at index target)]
    (into (empty target)
          (if (= "delete" (first op))
            (concat h t)
            (concat h [(run-op x op)] t)))))


(defmethod run-op
  "find"
  [target [_ pattern op]]
  {:pre [(or (nil? target) (sequential? target))]}
  (if-let [index (find-matching pattern target)]
    (run-op target ["nth" index op])
    (conj target (run-op pattern op))))


(defn patch [target diff]
  (if (nil? diff)
    target
    (if (and (map? target) (map? diff))
      (reduce-kv
        (fn [target k sub-diff]
          (if (vector? sub-diff)
            (if (= "delete" (first sub-diff))
              (dissoc target k)
              (assoc target k (run-op (get target k) sub-diff)))
            (update target k patch sub-diff)))
        target diff)
      diff)))
