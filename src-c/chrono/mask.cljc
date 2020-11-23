(ns chrono.mask
  (:require [chrono.util :as util]
            [clojure.string :as str]
            #?(:cljs [goog.string])
            #?(:cljs [goog.string.format]))
  (:refer-clojure :exclude [resolve]))

(defn- format-str [fmt & args]
  (apply
   #?(:clj  clojure.core/format
      :cljs goog.string/format)
   fmt
   args))

(defn parse [s fmt]
  (let [fmt (map #(cond-> % (vector? %) first) fmt)
        pat (map #(or (util/parse-patterns %) (util/sanitize %)) fmt)
        drop-pat (-> (remove keyword? fmt)
                     str/join
                     (#(str \["^0-9" % \]))
                     re-pattern)]
    (loop [s (some-> s (str/replace drop-pat ""))

           [f & rest-f :as fmts] fmt
           [p & rest-p :as pats] pat
           acc                   {}]
      (if-not (and s f)
        acc
        (let [ahead                  "(.+)?"
              pat                    (re-pattern (str "(" p ")" ahead))
              [match-s cur-s rest-s] (re-matches pat s)
              key?                   (contains? util/format-patterns f)
              f-len                  (util/format-patterns f)]
          (cond
            (and match-s
                 (or (not key?)
                     (= f-len (count cur-s))
                     (some? rest-s)
                     (not (re-matches (re-pattern p) (str cur-s \0)))))
            (recur rest-s rest-f rest-p (cond-> acc key? (assoc f (util/parse-int cur-s))))

            (not (or match-s key?)) (recur (str f s) fmts pats acc)
            (or match-s (= "0" s))  (assoc acc :not-parsed s)
            :else                   acc))))))

(defn build [t fmt]
  (reduce (fn [acc f]
            (let [kw (cond-> f (vector? f) first)
                  v  (get t kw)]
              (cond
                (not (contains? util/format-patterns kw))
                (str acc f)

                (number? v)
                (str acc (format-str (str "%0" (if (vector? f) (second f) (util/format-patterns f)) \d) v))

                (string? v)
                (str acc v)

                :else (reduced acc))))
          ""
          fmt))

(defn clean-build [t fmt]
  (let [clean-fmt
        (loop [acc       {:result []
                          :buff   []}
               [f & rfs] fmt]
          (cond
            (not-any? keyword? (cons f rfs))
            (-> acc
                (update :result concat (apply conj (:buff acc) f rfs))
                (assoc :buff []))

            (not (keyword? f)) (recur (update acc :buff conj f)
                                      rfs)
            (some? (get t f))  (recur (-> acc
                                          (update :result concat (conj (:buff acc) f))
                                          (assoc :buff []))
                                      rfs)
            :else              acc))]
    (build t (vec (:result clean-fmt)))))

(defn resolve [s fmt]
  (let [{:keys [not-parsed] :as p} (parse s fmt)]
    (str (build p fmt) not-parsed)))

(defn clean-resolve [s fmt]
  (clean-build (parse s fmt) fmt))
