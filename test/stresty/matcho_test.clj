(ns stresty.matcho-test
  (:require [stresty.matcho :as sut]
            [matcho.core :as matcho]
            [clojure.test :as t :refer [deftest]]
            [zen.core :as zen]))


(def ztx (zen/new-context))

(zen/read-ns ztx 'sty)
(defmacro match
  "Match against each pattern and assert with is"
  [x pat err]
  `(let [errors# (sut/match ztx {} ~x ~pat)]
     (matcho/match errors# ~err)
     errors#))

(deftest test-matcher
  (t/is (empty? (sut/match {} {:a 1} {:a 1})))

  (match {:a 1} {:a 2} [{:expected 2, :but 1, :path [:a]}])

  (match {:a 1} {:a 'sty/number?} empty?)
  (match {:a 1} {:a 'sty/integer?} empty?)

  ;; (match {:a "str"} {:a '(sty/regex "str")} empty?)

  (match {:a 200} {:a 'sty/ok?} empty?)

  (match {:a 204} {:a 'sty/ok?} empty?)

  ;; (match {:a "string"} {:a "#str\\w+"} empty?)

  ;; (match  {:a 404} {:a 'sty/2xx?} [{:expected 'sty/2xx? :but 404 :path [:a]}])

  ;; (match {:a {:b 1}} {:a {:c 'sty/nil?}} empty?)

  (match [{:n "a"} {:n "b"} {:n "c"}] [{:n "a"} 'sty/any? {:n "c"}] empty?)

  ;; (match {:body {:id "new-patient"}} {:body {:id "{user.data.patient_id}"}})

  )
