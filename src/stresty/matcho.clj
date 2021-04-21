(ns stresty.matcho
  (:require [clojure.string :as s]))

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

(defn- str->fn [fn-name]
  (if-let [fn (get fns fn-name)]
    fn
    (built-in-fn fn-name)))

(defmulti symbol-fn (fn [tp _] tp))

(defmethod symbol-fn 'stresty/string?
  [_ x]
  (if-not (string? x)
    {:expected "string" :but x}))

(defmethod symbol-fn 'stresty/distinct?
  [_ x]
  (if-not (distinct? x)
    {:expected "distinct" :but x}))

(defmethod symbol-fn 'stresty/double?
  [_ x]
  (if-not (double? x)
    {:expected "double" :but x}))

(defmethod symbol-fn 'stresty/empty?
  [_ x]
  (if-not (empty? x)
    {:expected "empty" :but x}))

(defmethod symbol-fn 'stresty/even?
  [_ x]
  (if-not (even? x)
    {:expected "even" :but x}))

(defn- smart-explain-data
  ([ctx p x]
   (smart-explain-data ctx p x {}))

  ([ctx p x m]
   (cond
     (and (string? p) (s/ends-with? p "?"))
     (if-let [f (str->fn p)]
       (smart-explain-data ctx f x {:fn-name p})
       {:expected (str p " is not a function") :but x})

     (and  (string? p) (s/starts-with? p "#"))
     (smart-explain-data ctx (java.util.regex.Pattern/compile (subs p 1)) x)

     (and (string? x) (instance? java.util.regex.Pattern p))
     (when-not (re-find p x)
       {:expected (str "match regexp: " p) :but x})

     (string? p)
     (let [p* (template ctx p)]
       (when-not (= p* x)
         {:expected p* :but x}))

     (symbol? p)
     (when-let [error (symbol-fn p x)]
       error)

     (fn? p)
     (when-not (p x)
       {:expected (or (:fn-name m) (pr-str p)) :but x})

     :else
     (when-not (= p x)
       {:expected p :but x}))))

(defn- match-recur [ctx errors path x pattern]
  (cond
    (and (map? x)
         (map? pattern))
    (do
      (reduce (fn [errors [k v]]
                (let [path (conj path k)
                      ev   (get x k)]
                  (match-recur ctx errors path ev v)))
              errors pattern))

    (and (sequential? pattern)
         (sequential? x))
    (reduce (fn [errors [k v]]
              (let [path (conj path k)
                    ev   (nth (vec x) k nil)]
                (match-recur ctx errors path ev v)))
            errors
            (map (fn [x i] [i x]) pattern (range)))

    :else (let [err (smart-explain-data ctx pattern x)]
            (if err
              (conj errors (assoc err :path path))
              errors))))

(defn match
  "Match against each pattern"
  [ctx x & patterns]
  (reduce (fn [acc pattern] (match-recur ctx acc [] x pattern)) [] patterns))

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

