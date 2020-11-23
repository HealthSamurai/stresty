(ns chrono.now
  (:require [chrono.ops :as ops]))

(defn tz-offset []
  #?(:clj (-> (java.time.ZonedDateTime/now)
              .getOffset
              .getTotalSeconds
              (->> (hash-map :sec))
              ops/normalize)
     :cljs (-> (js/Date.)
               .getTimezoneOffset
               -
               (->> (hash-map :min))
               ops/normalize)))

(defn local []
  (let [now (#?(:clj  java.time.LocalDateTime/now
                :cljs js/Date.))]
    {:year  #?(:clj  (-> now .getYear int)
               :cljs (-> now .getFullYear))
     :month #?(:clj  (-> now .getMonthValue int)
               :cljs (-> now .getMonth inc))
     :day   #?(:clj  (-> now .getDayOfMonth int)
               :cljs (-> now .getDate))
     :hour  #?(:clj  (-> now .getHour int)
               :cljs (-> now .getHours))
     :min   #?(:clj  (-> now .getMinute int)
               :cljs (-> now .getMinutes))
     :sec   #?(:clj  (-> now .getSecond int)
               :cljs (-> now .getSeconds))
     :ms    #?(:clj  (-> now .getNano (/ 1000000) int)
               :cljs (-> now .getMilliseconds))
     :tz    (:hour (tz-offset))}))

(defn utc []
  (let [now #?(:clj  (java.time.LocalDateTime/ofInstant
                      (java.time.Instant/now)
                      java.time.ZoneOffset/UTC)
               :cljs (js/Date.))]
    {:year  #?(:clj  (-> now .getYear int)
               :cljs (-> now .getUTCFullYear))
     :month #?(:clj  (-> now .getMonthValue int)
               :cljs (-> now .getUTCMonth inc))
     :day   #?(:clj  (-> now .getDayOfMonth int)
               :cljs (-> now .getUTCDate))
     :hour  #?(:clj  (-> now .getHour int)
               :cljs (-> now .getUTCHours))
     :min   #?(:clj  (-> now .getMinute int)
               :cljs (-> now .getUTCMinutes))
     :sec   #?(:clj  (-> now .getSecond int)
               :cljs (-> now .getUTCSeconds))
     :ms    #?(:clj  (-> now .getNano (/ 1000000) int)
               :cljs (-> now .getUTCMilliseconds))
     :tz    0}))

(defn today []
  (select-keys (local) [:year :month :day :tz]))

(defn utc-today []
  (select-keys (utc) [:year :month :day :tz]))
