(ns chrono.calendar
  (:require [chrono.util :as util]))

(def months
  {0  {:name "January" :days 31}
   1  {:name "February" :days 28 :leap 29}
   2  {:name "March" :days 31}
   3  {:name "April" :days 30}
   4  {:name "May" :days 31}
   5  {:name "June" :days 30}
   6  {:name "July" :days 31}
   7  {:name "August" :days 31}
   8  {:name "September" :days 30}
   9  {:name "October" :days 31}
   10 {:name "November" :days 30}
   11 {:name "December" :days 31}})

(def weeks
  {0 {:name "Sunday"}
   1 {:name "Monday"}
   2 {:name "Tuesday"}
   3 {:name "Wednesday"}
   4 {:name "Thursday"}
   5 {:name "Friday"}
   6 {:name "Saturday"}})

(def month-names
  {1  {:name "January" :short "Jan"}
   2  {:name "February" :short "Feb"}
   3  {:name "March" :short "Mar"}
   4  {:name "April" :short "Apr"}
   5  {:name "May" :short "May"}
   6  {:name "June" :short "June"}
   7  {:name "July" :short "July"}
   8  {:name "August" :short "Aug"}
   9  {:name "September" :short "Sep"}
   10 {:name  "October" :short "Oct"}
   11 {:name  "November" :short "Nov"}
   12 {:name  "December" :short "Dec"}})

(def month-names-ru
  {1  {:name "Январь"   :short "..."}
   2  {:name "Февраль"  }
   3  {:name "Март"     }
   4  {:name "Апрель"   }
   5  {:name "Май"      }
   6  {:name "Июнь"     }
   7  {:name "Июль"     }
   8  {:name "Август"   }
   9  {:name "Сентябрь" }
   10 {:name "Октябрь"  }
   11 {:name "Ноябрь"   }
   12 {:name "Декабрь"  }})


(defn shift-month [y m dir]
  (let [m (+ m (if (= :next dir) 1 -1))]
    (cond
      (< m  1) [(dec y) 12]
      (> m 12) [(inc y) 1]
      :else [y m])))

(defn for-month [y m & [fmt {today :today active :active}]]
  (let [start-day (util/day-of-week y m 1 fmt)
        start-month (if (= 1 m) 12 (dec m))
        pm-num-days (util/days-in-month {:year (if (= 1 m) (dec y) y), :month start-month})
        pm-last-day {:month start-month :day pm-num-days}
        start-cal (if (= 0 start-day)
                    {:month m :day 1}
                    {:month start-month :day (inc (- pm-num-days start-day))})
        num-d     (util/days-in-month {:year y, :month m})]
    {:year y :month m
     :cal (for [r (range 6)]
            (for [wd (range 7)]
              (let [idx (+ (* r 7) wd)
                    d (inc (- idx start-day))
                    cell (cond
                           (< idx start-day)
                           {:year (if (= 1 m) (dec y) y) :month start-month :day (+ (:day start-cal) idx)}

                           (> d num-d)
                           {:year (if (= m 12) (inc y) y) :month (if (= 12 m) 1 (inc m)) :day (inc (- idx start-day num-d))}

                           :else
                           {:year y :month m :day d :current true})]
                (if (and (= (:year cell) (:year active))
                         (= (:month cell) (:month active))
                         (= (:day cell) (:day active)))
                  (assoc cell :active true)
                  cell))))}))
