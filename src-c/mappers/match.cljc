(ns mappers.match
  (:require [clojure.string :as str]))


(defn match
  "Match against each pattern"
  [x pattern]
  (cond
    (and (map? x) (map? pattern))
    (loop [[[k v] & kvs] pattern]
      (if (nil? k)
        true
        (when (match (get x k) v)
          (recur kvs))))

    (and (sequential? pattern)
         (sequential? x))

    (if (and (empty? pattern)
             (empty? x))
      true
      (loop [[p & ps] pattern [x & xs] x]
        (if-not (match x p)
          false
          (if (empty? ps)
            true
            (recur ps xs)))))
    :else (= x pattern)))
