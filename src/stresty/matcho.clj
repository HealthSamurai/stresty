(ns stresty.matcho
  (:require [clojure.string :as s]
            [zen.core :as zen]))

(defn template [conf template]
  (s/replace
   template
   #"\{([a-zA-Z0-9-_\.]+)\}"
   #(let [template-segments (-> % second (s/split #"\."))]
      (->> template-segments
           (mapv keyword)
           (get-in conf)
           str))))

(def fns
  {"2xx?" #(and (>= % 200) (< % 300))
   "4xx?" #(and (>= % 400) (< % 500))
   "5xx?" #(and (>= % 500) (< % 600))})


(defn- built-in-fn [fn-name]
  (if-let [func (ns-resolve 'clojure.core (symbol fn-name))]
    #(func %)))

(defmulti predicate (fn [model _] (:zen/name model)))

(defmethod predicate 'sty/string?
  [_ x]
  (when-not (string? x)
    {:expected "string" :but x}))

(defmethod predicate 'sty/distinct?
  [_ x]
  (when-not (distinct? x)
    {:expected "distinct" :but x}))

(defmethod predicate 'sty/number?
  [_ x]
  (when-not (number? x)
    {:expected "number" :but x}))

(defmethod predicate 'sty/integer?
  [_ x]
  (when-not (integer? x)
    {:expected "integer" :but x}))

(defmethod predicate 'sty/ok?
  [_ x]
  (if-not (integer? x)
    {:expected "integer" :but x}
    (when-not (and (>= x 200) (< x 300))
      {:expected "expected >= 200 and <= 300" :but x})))

(defmethod predicate 'sty/any?
  [_ _])

;; {"2xx?" #
;;  "4xx?" #(and (>= % 400) (< % 500))
;;  "5xx?" #(and (>= % 500) (< % 600))}

(defmethod predicate 'sty/double?
  [_ x]
  (when-not (double? x)
    {:expected "double" :but x}))

(defmethod predicate 'sty/empty?
  [_ x]
  (when-not (empty? x)
    {:expected "empty" :but x}))

(defmethod predicate 'sty/even?
  [_ x]
  (when-not (even? x)
    {:expected "even" :but x}))

(defn- smart-explain-data
  ([ztx ctx p x]
   (smart-explain-data ztx ctx p x {}))

  ([ztx ctx p x m]
   (cond
     (and  (string? p) (s/starts-with? p "#"))
     (smart-explain-data ztx ctx (java.util.regex.Pattern/compile (subs p 1)) x)

     (string? p)
     (let [p* (template ctx p)]
       (when-not (= p* x)
         {:expected p* :but x}))

     (symbol? p)
     (if-let [model (zen/get-symbol ztx p)]
       (when-let [error (predicate model x)]
         error)
       {:syntax (str "Unrecognized symbol " p)})

     :else
     (when-not (= p x)
       {:expected p :but x}))))

(defn- match-recur [ztx ctx errors path x pattern]
  (cond
    (and (map? x)
         (map? pattern))

    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev   (get x k)]
                (match-recur ztx ctx errors path ev v)))
            errors pattern)

    (and (sequential? pattern)
         (sequential? x))
    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev   (nth (vec x) k nil)]
                (match-recur ztx ctx errors path ev v)))
            errors
            (map (fn [x i] [i x]) pattern (range)))

    :else (let [err (smart-explain-data ztx ctx pattern x)]
            (if err
              (conj errors (assoc err :path path))
              errors))))

(defn match
  "Match against each pattern"
  [ztx ctx x & patterns]
  (reduce (fn [acc pattern] (match-recur ztx ctx acc [] x pattern)) [] patterns))

(comment
  (def ctx* {:user {:data {:patient_id "pt-1"}}})

  (match {:user {:data {:patient_id "new-patient"}}}
         {:status 200,
          :body
          {:id           "new-patient",
           :resourceType "Patient",
           :meta
           {:lastUpdated "2020-11-19T10:15:36.124398Z",
            :createdAt   "2020-11-19T10:15:36.124398Z",
            :versionId   "483"}}}
         {:status 200
          :body   {:id 'stresty/string?}}
         )
  )

