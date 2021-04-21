(ns matcho-test
  (:require [matcho :as sut]
            [clojure.test :refer :all]))


;; (defmacro match
;;   "Match against each pattern and assert with is"
;;   [x & pattern]
;;   `(let [x# ~x
;;          patterns# [~@pattern]
;;          errors# (apply sut/match x# patterns#)]
;;      (if-not (empty? errors#)
;;        (is false (pr-str errors# x# patterns#))
;;        (is true))))

(deftest test-matcho
  (is (empty? (sut/match {} {:a 1} {:a 1})))

  (is (=
       (sut/match {} {:a 1} {:a 2})
       [{:expected 2 :but 1 :path [:a]}]))

  (is (empty? (sut/match {} {:a 1} {:a number?})))

  (is (empty? (sut/match {} {:a 1} {:a "number?"})))

  (is (empty? (sut/match {} {:a "str"} {:a "#str"})))

  (is (empty? (sut/match {} {:a 200} {:a "2xx?"})))

  (is (empty? (sut/match {} {:a 204} {:a "2xx?"})))

  (is (empty? (sut/match {} {:a "string"} {:a "#str\\w+"})))

  (is (= (sut/match {} {:a 404} {:a "2xx?"}) [{:expected "2xx?" :but 404 :path [:a]}]))

  (is (empty? (sut/match {} {:a {:b 1}} {:a {:c "nil?"}})))

  (is (empty? (sut/match {} [{:n "a"} {:n "b"} {:n "c"}] [{:n "a"} "any?" {:n "c"}])))

  (is (empty? (sut/match {:user {:data {:patient_id "new-patient"}}}
                         {:body {:id "new-patient"}}
                         {:body {:id "{user.data.patient_id}"}})))

  )
