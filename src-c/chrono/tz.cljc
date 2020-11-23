(ns chrono.tz
  (:require [chrono.ops :as ops]
            [chrono.util :as util]))


;; Rule  US   1967 2006  -   Oct lastSun  2:00  0    S
;; Rule  US   1967 1973  -   Apr lastSun  2:00  1:00 D
;; Rule  US   1974 only  -   Jan 6        2:00  1:00 D
;; Rule  US   1975 only  -   Feb 23       2:00  1:00 D
;; Rule  US   1976 1986  -   Apr lastSun  2:00  1:00 D
;; Rule  US   1987 2006  -   Apr Sun>=1   2:00  1:00 D
;; Rule  US   2007 max   -   Mar Sun>=8   2:00  1:00 D
;; Rule  US   2007 max   -   Nov Sun>=1   2:00  0    S

;; Zone America/New_York
;; -5:00	US	E%sT	1920
;; -5:00	NYC	E%sT	1942
;; -5:00	US	E%sT	1946
;; -5:00	NYC	E%sT	1967
;; -5:00	US	E%sT

;; rules from tzdb like sun >= 8

(defmethod ops/day-saving
  :ny
  [_ y]
  (assert (> y 2006) "Not impl.")
  {:offset 5
   :ds -1
   :in {:year y :month 3 :day (util/more-or-eq y 3 0 8) :hour 2 :min 0}
   :out {:year y :month 11 :day (util/more-or-eq y 11 0 1) :hour 2 :min 0}})

;; https://alcor.concordia.ca/~gpkatch/gdate-algorithm.html
;; https://alcor.concordia.ca/~gpkatch/gdate-method.html

(defn *ddd [y]
  (+ (* 365 y)
     (quot y 4)
     (quot y -100)
     (quot y 400)))

(defn *mmm [m]
  (->
   (* 306 m)
   (+ 5)
   (quot 10)))

(defn g [y m d]
  (let [m (rem (+ m 9) 12)
        y (- y (quot m 10))]
    (+ (*ddd y) (*mmm m) (dec d))))


(defn d [g]
  (let [y (-> (* 10000 g)
              (+ 14780)
              (quot 3652425))
        ddd (- g (*ddd y))
        y   (if (< ddd 0) (dec y) y)
        ddd (if (< ddd 0) (- g (*ddd y)) ddd)
        mi (-> (* 100 ddd)
               (+ 52)
               (quot 3060))
        mm (-> (+ mi 2)
               (rem 12)
               (+ 1))
        y (+ y (-> (+ mi 2) (quot 12)))
        dd  (+ ddd (- (*mmm mi)) 1)]
    [y mm dd]))

(comment
  (g 2018 3 1)
  (d (g 2018 1 1))
  (d (g 2018 3 5))
  (d (g 2018 7 2))
  (d (- (g 2018 1 1) 1))
  (d (+ (g 2017 12 31) 1))
  (d (+ (g 2017 12 31) 370)))
