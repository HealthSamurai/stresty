(ns matcho-test
  (:require [matcho :as sut]
            [clojure.test :refer :all]))


(defmacro match
  "Match against each pattern and assert with is"
  [x & pattern]
  `(let [x# ~x
         patterns# [~@pattern]
         errors# (apply sut/match x# patterns#)]
     (if-not (empty? errors#)
       (is false (pr-str errors# x# patterns#))
       (is true))))

(deftest test-matcho
  (is (empty? (sut/match {:a 1} {:a 1})))

  (match
   (sut/match {:a 1} {:a 2})
   [{:expected 2 :path [:a]}])


  (is (empty? (sut/match {:a 1} {:a number?})))

  (is (empty? (sut/match {:a 1} {:a "number?"})))

  (is (empty? (sut/match {:a "str"} {:a "#str"})))

  (is (empty? (sut/match {:a 200} {:a "2xx?"})))

  (is (empty? (sut/match {:a 204} {:a "2xx?"})))

  (is (empty? (sut/match {:a "string"} {:a "#str\\w+"})))

  )

