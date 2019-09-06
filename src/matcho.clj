
(ns matcho)

(defn smart-explain-data [p x]
  (cond

    (and (string? x) (instance? java.util.regex.Pattern p))
    (when-not (re-find p x)
      {:expected (str "match regexp: " p) :but x})

    (fn? p)
    (when-not (p x)
      {:expected (pr-str p) :but x})

    :else (when-not (= p x)
            {:expected p :but x})))

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



