(ns chrono.crono
  (:require [chrono.core :as ch]
            [chrono.now :as now]
            [chrono.util :as util]))

(def needed-for
  {:month [:year :month]
   :day [:year :month :day]
   :hour [:year :month :day :hour]
   :min [:year :month :day :hour :min]
   :sec [:year :month :day :hour :min :sec]})

(def default-at
  {:hour {:min 0}
   :min {:sec 0}})

(defn next-time-assumption [current-time {every :every at :at}])

(def days-of-week [:sunday :monday :tuesday :wednesday :thursday :friday :saturday])

(defn *next-time
  ([current-time {every :every at :at :as when}]
   (let [every (keyword every)
         at (or at (get default-at every))
         _ (if (nil? every) (throw (ex-info ":every must be specified" {:when when})))
         _ (if (nil? at) (throw (ex-info ":at must be specified" {:when when})))
         at (if (map? at) [at] at)
         assumptions (map #(merge (select-keys current-time (get needed-for every)) %) at)]
     (if (nil? (first (filter #(ch/< current-time %) assumptions)))
       (ch/+ (first assumptions) {every 1})
       (first (filter #(ch/< current-time %) assumptions))))))

(defn validate-cfg [cfg]
  (assert (:every cfg) ":every must be specified")
  (assert (contains? #{:month :day :hour :min :sunday :monday :tuesday :wednesday :thursday :friday :saturday}
                     (keyword (:every cfg)))
          ":every must one of [month day hour min sunday monday tuesday wednesday thursday friday saturday]"))

(defn next-time
  ([cfg] (next-time (now/utc) cfg))
  ([current-time cfg]
   (validate-cfg cfg)
   (if (contains? (set days-of-week) (keyword (:every cfg)))
     (first
      (filter
       #(= (util/day-of-week (:year %) (:month %) (:day %))
           (.indexOf days-of-week (keyword (:every cfg))))
       (drop 1 (iterate
                (fn [current-time] (*next-time current-time (assoc cfg :every :day)))
                current-time))))
     (*next-time current-time cfg))))

(defn now?
  ([cfg] (now? (now/utc) cfg))
  ([current-time {every :every until :until :as when}]
   (if until
     (let [utmost-time (merge (select-keys current-time (get needed-for every)) until)]
       (ch/< current-time utmost-time))
     true)))

(comment
  (next-time {} )

  (let [n (now/utc)
        cfg {:every "wednesday" :at {:hour 15}}]
    (prn (next-time n cfg))
    (prn (another-next-time n  cfg)))


  (= {:year 2020 :month 5 :day 18 :hour 18}
     (next-time #_(now/utc) {:year 2020 :month 5 :day 18 :hour 18 :min 44}
                {:every "tuesday" :at {:hour 12 :min 10}}))

  (= {:year 2020 :month 1 :day 1 :hour 12}
     (next-time {:year 2020 :month 1 :day 1 :hour 11}
                {:every "day" :at {:hour 12}}))

  (= {:year 2020 :month 1 :day 2 :hour 12}
     (next-time {:year 2020 :month 1 :day 1 :hour 12 :min 10}
                {:every :day :at {:hour 12}}))

  (= {:year 2020 :month 1 :day 1 :hour 14}
     (next-time-2 {:year 2020 :month 1 :day 1 :hour 13}
                  {:every :day :at [{:hour 12}
                                    {:hour 14}]}))

  (= {:year 2020 :month 1 :day 1 :hour 12}
     (next-time {:year 2020 :month 1 :day 1 :hour 11}
                {:every :day :at {:hour 12}}))

  (= {:year 2020 :month 1 :day 2 :hour 12}
     (next-time {:year 2020 :month 1 :day 1 :hour 12 :min 10}
                {:every :day :at {:hour 12}}))

  (= {:year 2020 :month 1 :day 1 :hour 14}
     (next-time-2 {:year 2020 :month 1 :day 1 :hour 13}
                  {:every :day :at [{:hour 12}
                                    {:hour 14}]}))

  (= {:year 2020 :month 1 :day 1 :hour 10 :min 30}
     (next-time-2 {:year 2020 :month 1 :day 1 :hour 10 :min 13}
                  {:every :hour :at [{:min 0} {:min 30}]}))

  (= {:year 2020 :month 1 :day 1 :hour 12}
     (next-time-2 {:year 2020 :month 1 :day 1 :hour 11 :min 43}
                  {:every :hour :at [{:min 0} {:min 30}]}))

  (= true
     (now? {:year 2020 :month 1 :day 1 :hour 12 :min 31}
           {:every :day
            :at {:hour 12}
            :until {:hour 12 :min 30}}))

  )
