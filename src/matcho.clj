(ns matcho
  (:require [clojure.string :as s])
  (:require [stresty]))

(def fns
  {"2xx?" #(and (>= % 200) (< % 300))
   "4xx?" #(and (>= % 400) (< % 500))
   "5xx?" #(and (>= % 500) (< % 600))})



(defn built-in-fn [fn-name]
  (if-let [func (ns-resolve 'clojure.core (symbol fn-name))]
    #(func %)))

(defmulti symbol-fn (fn [tp _] tp))

(defmethod symbol-fn 'stresty/string?
  [_ x]
  (if (-> x string? not)
    {:expected "string" :but x}))

(defmethod symbol-fn 'stresty/distinct?
  [_ x]
  (if (-> x distinct? not)
    {:expected "distinct" :but x}))

(defmethod symbol-fn 'stresty/double?
  [_ x]
  (if (-> x double? not)
    {:expected "double" :but x}))

(defmethod symbol-fn 'stresty/empty?
  [_ x]
  (if (-> x empty? not)
    {:expected "empty" :but x}))

(defmethod symbol-fn 'stresty/even?
  [_ x]
  (if (-> x even? not)
    {:expected "even" :but x}))

(defn smart-explain-data
  ([p x]
   (smart-explain-data p x {}))

  ([p x m]
   (cond
     (and (string? p) (s/ends-with? p "?"))
     (if-let [f (str->fn p)]
       (smart-explain-data f x {:fn-name p})
       {:expected (str p " is not a function") :but x})

     (and  (string? p) (s/starts-with? p "#"))
     (smart-explain-data (java.util.regex.Pattern/compile (subs p 1)) x)

     (and (string? x) (instance? java.util.regex.Pattern p))
     (when-not (re-find p x)
       {:expected (str "match regexp: " p) :but x})

     (symbol? p)
     (if-let [error (symbol-fn p x)]
       error)
     
     (fn? p)
     (when-not (p x)
       {:expected (or (:fn-name m) (pr-str p)) :but x})

     :else (when-not (= p x)
             {:expected p :but x}))))

(defn- match-recur [errors path x pattern]
  (cond
    (and (map? x)
         (map? pattern))
    (let []
      (reduce (fn [errors [k v]]
                (let [path (conj path k)
                      ev   (get x k)]
                  (match-recur errors path ev v)))
              errors pattern))

    (and (sequential? pattern)
         (sequential? x))
    (let []
      (reduce (fn [errors [k v]]
                (let [path (conj path k)
                      ev   (nth (vec x) k nil)]
                  (match-recur errors path ev v)))
              errors
              (map (fn [x i] [i x]) pattern (range))))

    :else (let [err (smart-explain-data pattern x)]
            (if err
              (conj errors (assoc err :path path))
              errors))))

(defn- match-recur-strict [errors path x pattern]
  (cond
    (and (map? x)
         (map? pattern))
    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev   (get x k)]
                (match-recur-strict errors path ev v)))
            errors pattern)

    (and (sequential? pattern)
         (sequential? x))
    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev   (nth (vec x) k nil)]
                (match-recur-strict errors path ev v)))
            (if (= (count pattern) (count x))
              errors
              (conj errors {:expected "Same number of elements in sequences"
                            :but      (str "Got " (count pattern)
                                           " in pattern and " (count x) " in x")
                            :path     path}))
            (map (fn [x i] [i x]) pattern (range)))

    :else (let [err (smart-explain-data pattern x)]
            (if err
              (conj errors (assoc err :path path))
              errors))))

(defn match
  "Match against each pattern"
  [x & patterns]
  (reduce (fn [acc pattern] (match-recur acc [] x pattern)) [] patterns))

(comment
  (match {:status 201
          :body {:id "pt-1"
                 :meta {:versionId "344"}}}
         {:status 201
          :body {:id "pt-1"
                 :meta {:versionId stresty/string?}}})
  )
