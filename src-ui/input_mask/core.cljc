(ns input-mask.core
  "Ported from https://github.com/insin/inputmask-core"
  (:require [clojure.string :as str]))

(def escape-char "\\")
(def default-placeholder-char "_")


(def digit-re #"^\d$")
(def letter-re #"^[A-Za-z]$")
(def alpha-numeric-re #"^[\dA-Za-z]$")


(defn assoc-in*
  [m [k & ks] v]
  (let [assoc
        (fn [m k v]
          (if (and (int? k)
                   (or (nil? m) (vector? m))
                   (>= k (count m)))
            (assoc (into (or m []) (repeat (- k (count m)) nil)) k v)
            (assoc m k v)))]
    (if ks
      (assoc m k (assoc-in* (get m k) ks v))
      (assoc m k v))))


(def default-format-characters
  {"*" {:validate #(re-matches alpha-numeric-re %)}
   "1" {:validate #(re-matches digit-re %)}
   "a" {:validate #(re-matches letter-re %)}
   "A" {:validate #(re-matches letter-re %) :transform str/upper-case}
   "#" {:validate #(re-matches alpha-numeric-re %) :transform str/upper-case}})


(defn parse-pattern
  [pattern]
  (let [source-chars (map str (:source pattern))]
    (loop [[char & rest-chars] source-chars
           index   0
           pattern pattern]
      (if (nil? char)
        (do (assert (:first-editable-index pattern)
                    (str "Pattern \"" (:source pattern) "\" does not contain any editable characters."))
            (assoc pattern :length (count (:pattern pattern))))
        (cond
          (= escape-char char)
          (do (assert (seq rest-chars) (str "Pattern ends with a raw " escape-char))
              (recur
                (rest rest-chars)
                (inc index)
                (update pattern :pattern conj (first rest-chars))))
          (get (:format-characters pattern) char)
          (recur
            rest-chars
            (inc index)
            (-> pattern
                (update :pattern conj char)
                (update :first-editable-index #(or % index))
                (assoc :last-editable-index index)
                (update :editable-indices conj index)))
          :else
          (recur
            rest-chars
            (inc index)
            (update pattern :pattern conj char)))))))


(defn validate
  [pattern char index]
  (let [validate (get-in pattern [:format-characters (get-in pattern [:pattern index]) :validate])]
    (validate char)))


(defn transform
  [pattern char index]
  (let [transform (get-in pattern [:format-characters (get-in pattern [:pattern index]) :transform] identity)]
    (transform char)))


(defn format-value [pattern value]
  (loop [i            0
         value-index  0
         value-buffer (vec (repeat (:length pattern) nil))]
    (if (< i (:length pattern))
      (if (get-in pattern [:editable-indices i])
        (if (and (:revealing? pattern)
                 (<= (count value) value-index)
                 (not (when-let [char (nth value value-index nil)]
                        (validate pattern char i))))
          value-buffer
          (recur
            (inc i)
            (inc value-index)
            (assoc value-buffer i (if (and (> (count value) value-index)
                                           (validate pattern (nth value value-index) i))
                                    (transform pattern (nth value value-index) i)
                                    (:placeholder-char pattern)))))
        (recur
          (inc i)
          (if (and (> (count value) value-index)
                   (= (nth value value-index) (get-in pattern [:pattern i])))
            (inc value-index)
            value-index)
          (assoc value-buffer i (get-in pattern [:pattern i]))))
      value-buffer)))


(defn make-pattern [source format-characters placeholder-char revealing?]
  (assert source "You must provide a pattern.")

  (assert (or (empty? placeholder-char) (= 1 (count placeholder-char)))
          "PlaceholderChar should be a single character.")

  (parse-pattern
    {:placeholder-char     (or placeholder-char default-placeholder-char)
     :format-characters    (or format-characters default-format-characters)
     :source               source
     :pattern              []
     :length               0
     :first-editable-index nil
     :last-editable-index  nil
     :editable-indices     #{}
     :revealing?           (boolean revealing?)}))


(defn set-value [input-mask value]
  (assoc input-mask
    :value (format-value (:pattern input-mask)
                         (map str value))))


(defn get-raw-value [input-mask]
  (->> (:value input-mask)
       (keep-indexed
         (fn [i char]
           (when (get-in input-mask [:pattern :editable-indices i])
             char)))
       (str/join "")))


(defn get-value [input-mask]
  (if (get-in input-mask [:pattern :revealing?])
    (->> (map str (get-raw-value input-mask))
         (format-value (:pattern input-mask))
         (not-empty)
         (str/join ""))
    (when (->> (map-indexed vector (:value input-mask))
               (some (fn [[i char]]
                       (when (get-in input-mask [:pattern :editable-indices i])
                         (not= char (get-in input-mask [:pattern :placeholder-char]))))))
      (str/join "" (:value input-mask)))))


(defn input [input-mask char]
  (if (= (get-in input-mask [:selection :start])
         (get-in input-mask [:selection :end])
         (get-in input-mask [:pattern :length]))
    false
    (let [input-index (max (get-in input-mask [:selection :start])
                           (get-in input-mask [:pattern :first-editable-index]))]
      (if (and (get-in input-mask [:pattern :editable-indices input-index])
               (not (validate (:pattern input-mask) char input-index)))
        false
        (let [input-mask (if (get-in input-mask [:pattern :editable-indices input-index])
                           (assoc-in* input-mask [:value input-index]
                                      (transform (:pattern input-mask) char input-index))
                           input-mask)
              input-mask (reduce
                           (fn [input-mask i]
                             (if (get-in input-mask [:pattern :editable-indices i])
                               (assoc-in* input-mask [:value i] (get-in input-mask [:pattern :placeholder-char]))
                               input-mask))
                           input-mask
                           (range
                             (inc input-index)
                             (get-in input-mask [:selection :end])))
              input-mask (assoc input-mask :selection {:start (inc input-index)
                                                       :end   (inc input-index)})
              input-mask (loop [input-mask input-mask]
                           (if (and (> (get-in input-mask [:pattern :length]) (get-in input-mask [:selection :start]))
                                    (not (get-in input-mask [:pattern :editable-indices (get-in input-mask [:selection :start])])))
                             (recur (-> input-mask
                                        (update-in [:selection :start] inc)
                                        (update-in [:selection :end] inc)))
                             input-mask))]
          input-mask)))))


(defn backspace [input-mask]
  (if (= (get-in input-mask [:selection :start])
         (get-in input-mask [:selection :end])
         0)
    false
    (let [input-mask (if (= (get-in input-mask [:selection :start])
                            (get-in input-mask [:selection :end]))
                       (let [backspace-index (dec (get-in input-mask [:selection :start]))]
                         (-> input-mask
                             (update-in [:selection :start] dec)
                             (update-in [:selection :end] dec)
                             (cond-> (get-in input-mask [:pattern :editable-indices backspace-index])
                               (assoc :value (if (get-in input-mask [:pattern :revealing?])
                                               (vec (take backspace-index (:value input-mask)))
                                               (assoc (:value input-mask)
                                                 backspace-index (get-in input-mask [:pattern :placeholder-char])))))))
                       (reduce
                         (fn [input-mask i]
                           (if (get-in input-mask [:pattern :editable-indices i])
                             (assoc-in* input-mask [:value i] (get-in input-mask [:pattern :placeholder-char]))
                             input-mask))
                         (assoc-in* input-mask [:selection :end] (get-in input-mask [:selection :start]))
                         (range (get-in input-mask [:selection :start])
                                (get-in input-mask [:selection :end]))))]
      input-mask)))


(defn paste [input-mask input-str]
  (let [input-chars (mapv str input-str)]
    (if (and (< (get-in input-mask [:selection :start])
                (get-in input-mask [:pattern :first-editable-index]))
             (not (every?
                    #(= (nth input-chars %) (get-in input-mask [:pattern :pattern %]))
                    (range 0 (- (get-in input-mask [:pattern :first-editable-index])
                                (get-in input-mask [:selection :start]))))))
      false
      (let [input-chars (if (< (get-in input-mask [:selection :start])
                               (get-in input-mask [:pattern :first-editable-index]))
                          (vec (drop (- (get-in input-mask [:pattern :first-editable-index])
                                        (get-in input-mask [:selection :start]))
                                     input-chars))
                          input-chars)
            input-mask  (if (< (get-in input-mask [:selection :start])
                               (get-in input-mask [:pattern :first-editable-index]))
                          (assoc-in* input-mask [:selection :start]
                                     (get-in input-mask [:pattern :first-editable-index]))
                          input-mask)]
        (loop [i          0
               input-mask input-mask]
          (if (and (< i (count input-chars))
                   (<= (get-in input-mask [:selection :start])
                       (get-in input-mask [:pattern :last-editable-index])))
            (let [new-input-mask (input input-mask (nth input-chars i))]
              (if new-input-mask
                (recur (inc i)
                       new-input-mask)
                (if (and (pos? (get-in input-mask [:selection :start]))
                         (let [pattern-index (dec (get-in input-mask [:selection :start]))]
                           (and (not (get-in input-mask [:pattern :editable-indices pattern-index]))
                                (= (nth input-chars i) (get-in input-mask [:pattern :pattern pattern-index])))))
                  (recur (inc i)
                         input-mask)
                  false)))
            input-mask))))))


(defn set-selection [input-mask selection]
  (let [input-mask (assoc input-mask :selection selection)]
    (if (= (get-in input-mask [:selection :start])
           (get-in input-mask [:selection :end]))
      (if (< (get-in input-mask [:selection :start])
             (get-in input-mask [:pattern :first-editable-index]))
        (assoc input-mask :selection {:start (get-in input-mask [:pattern :first-editable-index])
                                      :end   (get-in input-mask [:pattern :first-editable-index])})
        (reduce
          (fn [input-mask index]
            (if (or (and (get-in input-mask [:pattern :editable-indices (dec index)])
                         (not= (get-in input-mask [:value (dec index)]) (get-in input-mask [:pattern :placeholder-char])))
                    (= index (get-in input-mask [:pattern :first-editable-index])))
              (reduced (assoc input-mask :selection {:start index
                                                     :end   index}))
              input-mask))
          input-mask
          (reverse (range (get-in input-mask [:pattern :first-editable-index])
                          (inc (get-in input-mask [:selection :start]))))))
      false)))


(defn make-input-mask [{:keys [pattern format-characters placeholder-char revealing? selection value]
                        :or   {selection {:start 0 :end 0} value ""}}]

  (let [pattern (make-pattern pattern
                              (merge default-format-characters format-characters)
                              placeholder-char
                              revealing?)]
    (-> {:pattern     pattern
         :selection   selection
         :empty-value (not-empty (str/join "" (format-value pattern [])))}
        (set-value value))))



