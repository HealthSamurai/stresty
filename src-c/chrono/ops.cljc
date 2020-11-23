(ns chrono.ops
  (:require [chrono.util :as u]))

(defn gen-norm [unit next-unit proportion min-value next-min-value]
  (fn [t]
    (if-let [unit-value (get t unit)]
      (let [norm-unit-value     (- unit-value min-value)
            borrow?             (neg? norm-unit-value)
            unit-new-value      (+ min-value (mod norm-unit-value proportion))
            next-unit-value     (get t next-unit next-min-value)
            next-unit-new-value (+ next-unit-value
                                   (cond-> (quot norm-unit-value proportion)
                                     borrow? dec))]
        (assoc t
               unit unit-new-value
               next-unit next-unit-new-value))
      t)))

(def normalize-ms (gen-norm :ms    :sec  1000 0 0))
(def normalize-s  (gen-norm :sec   :min  60   0 0))
(def normalize-mi (gen-norm :min   :hour 60   0 0))
(def normalize-h  (gen-norm :hour  :day  24   0 0))
(def normalize-m  (gen-norm :month :year 12   1 1))

(defn days-and-months [y m d]
  (if (<= 1 d 27)
    [y m d]
    (cond
      (> d 0)
      (let [num-days (u/days-in-month {:year y, :month m})
            dd (- d num-days)]
        (if (<= d num-days)
          [y m d]
          (if (= m 12)
            (days-and-months (inc y) 1 dd)
            (days-and-months y (inc m) dd))))

      (<= d 0)
      (let [[num-days ny nm] (if (= m 1)
                               [(u/days-in-month {:year (dec y), :month 12}) (dec y) 12]
                               [(u/days-in-month {:year y, :month (dec m)}) y (dec m)])
            dd (+ num-days d)]
        (if (< 0 dd)
          [ny nm dd]
          (days-and-months ny nm dd))))))

(defn normalize-d  [x]
  (if (and (:year x) (:month x) (:day x))
    (let [[y m d] (days-and-months (:year x) (:month x) (:day x))]
      (assoc x :year y :month m :day d))
    x))

(defmulti normalize-rule (fn [unit _] unit))
(defmethod normalize-rule :default [_ t] t)
(defmethod normalize-rule :ms [_ t] (normalize-ms t))
(defmethod normalize-rule :sec [_ t] (normalize-s t))
(defmethod normalize-rule :min [_ t] (normalize-mi t))
(defmethod normalize-rule :hour [_ t] (normalize-h t))
(defmethod normalize-rule :day [_ t] (normalize-d t))
(defmethod normalize-rule :month [_ t] (normalize-m t))

(def defaults-units  [[:year 0] [:month 1] [:day 1] [:hour 0] [:min 0] [:sec 0] [:ms 0]])
(defn custom-units [t]
  (let [units-to-ignore (into #{} (conj (map first defaults-units) :tz))
        current-units (into #{} (keys t))]
    (into [] (remove units-to-ignore current-units))))

(defn ordered-rules [t]
  (let [init [:ms :sec :min :hour :month]
        with-custom (apply conj (custom-units t) init)]
    (conj with-custom :day)))

(declare to-utc)
(declare to-tz)

(defn normalize [{:keys [tz] :as t}]
  (let [rules           (ordered-rules t)
        normalized-time (reduce (fn [t unit] (normalize-rule unit t)) t rules)]
    (into {}
          (remove (every-pred (comp not #{:tz} key) (comp zero? val)))
          normalized-time)))

(def ^:private default-time {:year 0 :month 1 :day 1 :hour 0 :min 0 :sec 0 :ms 0})

(defn- init-plus [{:keys [tz] :as r} i]
  (let [i-r-tz (to-tz i tz)]
    (into (if tz {:tz tz} {})
          (map (fn [k] {k (+ (get r k 0) (get i-r-tz k 0))}))
          (disj (set (concat (keys r) (keys i-r-tz))) :tz))))

(defn plus
  ([]           default-time)
  ([x]          x)
  ([x y]        (normalize (init-plus x y)))
  ([x y & more] (reduce plus (plus x y) more)))

(defn invert [x]
  (reduce
   (fn [x k] (cond-> x
               (contains? x k)
               (update k -)))
   x
   [:year :month :day :hour :min :sec :ms]))

(defn minus
  ([]           default-time)
  ([x]          x)
  ([x y]        (normalize (init-plus x (invert y))))
  ([x y & more] (reduce minus (minus x y) more)))

(def to-normalized-utc (comp normalize #(to-tz % 0)))

(defn- after? [t t']
  (loop [[[p s] & ps] defaults-units]
    (let [t->tp #(get % p s)
          tp (t->tp t)
          tp' (t->tp t')]
      (cond
        (> tp tp') true
        (= tp tp') (and (seq ps) (recur ps))
        :else false))))

(defn eq? [& ts]
  (apply = (map to-normalized-utc ts)))

(def not-eq? (complement eq?))

(defn gt
  ([_] true)
  ([x y] (after? (to-normalized-utc x) (to-normalized-utc y)))
  ([x y & more]
   (if (gt x y)
     (if (next more)
       (recur y (first more) (next more))
       (gt y (first more)))
     false)))

(defn denormalised-gt
  ([_] true)
  ([x y] (after? x y))
  ([x y & more]
   (if (denormalised-gt x y)
     (if (next more)
       (recur y (first more) (next more))
       (denormalised-gt y (first more)))
     false)))

(defn gte
  ([_] true)
  ([x y] (or (gt x y) (eq? x y)))
  ([x y & more]
   (if (gte x y)
     (if (next more)
       (recur y (first more) (next more))
       (gte y (first more)))
     false)))

(defn lt
  ([_] true)
  ([x & args] (apply (complement gte) x args)))

(defn lte
  ([_] true)
  ([x & args] (apply (complement gt) x args)))

(defn denormalised-lte
  ([_] true)
  ([x & args] (apply (complement denormalised-gt) x args)))

(defn cmp [x y]
  (cond
    (eq? x y) 0
    (gt x y)  1
    (lt x y)  -1))

(defmulti day-saving "[tz y]" (fn [tz _] tz))

(defmethod day-saving
  :ny
  [_ y]
  (assert (> y 2006) "Not impl.")
  {:offset 5
   :ds -1
   :in {:year y :month 3 :day (u/more-or-eq y 3 0 8) :hour 2 :min 0}
   :out {:year y :month 11 :day (u/more-or-eq y 11 0 1) :hour 2 :min 0}})

(defn *day-saving-with-utc [tz y]
  (let [ds (day-saving tz y)]
    (assoc ds
           :in-utc (plus (:in ds) {:hour (:offset ds)})
           :out-utc (plus (:out ds) {:hour (+ (:offset ds) (:ds ds))}))))

(def day-saving-with-utc (memoize *day-saving-with-utc))

(defn kw-tz->utc0 [t] ;; TODO: make this work for any utc offset
  (let [ds (day-saving-with-utc (:tz t) (:year t))
        off (if (or (denormalised-lte t (:in ds)) (denormalised-gt t (:out ds)))
              (:offset ds)
              (+ (:offset ds) (:ds ds)))]
    (-> (dissoc t :tz)
        (plus {:hour off}))))

(defn utc0->kw-tz [t tz] ;; TODO: make this work for any utc offset
  (let [ds (day-saving-with-utc tz (:year t))
        off (if (or (denormalised-lte t (:in-utc ds)) (denormalised-gt t (:out-utc ds)))
              (:offset ds)
              (+ (:offset ds) (:ds ds)))]
    (-> (plus t {:hour (- off)})
        (assoc :tz tz))))

(defn to-tz [{:keys [tz] :as t} dtz]
  (cond
    (nil? dtz) (dissoc t :tz)
    (nil? tz)  (assoc t :tz dtz)
    :else
    (let [d (- (if (keyword? dtz) 0 dtz)
               (if (keyword? tz)  0 tz))]
      (cond-> t
        (keyword? tz)   kw-tz->utc0
        :always         (dissoc :tz)
        (not (zero? d)) (plus {:hour d})
        (keyword? dtz)  (utc0->kw-tz dtz)
        :always         (assoc :tz dtz)))))

(defn to-utc [t] (to-tz t 0))
