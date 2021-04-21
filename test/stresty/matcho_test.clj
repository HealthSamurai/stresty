(ns stresty.matcho-test
  (:require [stresty.matcho :as sut]
            [clojure.test :as t]))


;; (defmacro match
;;   "Match against each pattern and assert with is"
;;   [x & pattern]
;;   `(let [x# ~x
;;          patterns# [~@pattern]
;;          errors# (apply sut/match x# patterns#)]
;;      (if-not (empty? errors#)
;;        (t/is false (pr-str errors# x# patterns#))
;;        (t/is true))))

(t/deftest test-matcho
  (t/is (empty? (sut/match {} {:a 1} {:a 1})))

  (t/is (=
       (sut/match {} {:a 1} {:a 2})
       [{:expected 2 :but 1 :path [:a]}]))

  (t/is (empty? (sut/match {} {:a 1} {:a 'sty/number?})))

  (t/is (empty? (sut/match {} {:a 1} {:a "number?"})))

  (t/is (empty? (sut/match {} {:a "str"} {:a "#str"})))

  (t/is (empty? (sut/match {} {:a 200} {:a "2xx?"})))

  (t/is (empty? (sut/match {} {:a 204} {:a "2xx?"})))

  (t/is (empty? (sut/match {} {:a "string"} {:a "#str\\w+"})))

  (t/is (= (sut/match {} {:a 404} {:a "2xx?"}) [{:expected "2xx?" :but 404 :path [:a]}]))

  (t/is (empty? (sut/match {} {:a {:b 1}} {:a {:c "nil?"}})))

  (t/is (empty? (sut/match {} [{:n "a"} {:n "b"} {:n "c"}] [{:n "a"} "any?" {:n "c"}])))

  (t/is (empty? (sut/match {:user {:data {:patient_id "new-patient"}}}
                         {:body {:id "new-patient"}}
                         {:body {:id "{user.data.patient_id}"}})))

  )
