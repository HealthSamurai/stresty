(ns stresty.sci-test
  (:require [stresty.sci :as sut]
            [matcho.core :as matcho]
            [clojure.test :as t]))

(t/deftest test-sci
  (matcho/match
   (sut/eval-form {:namespaces {'sty {'case {:a {:b 1}}}}}
                  '(get-in sty/case [:a :b]))

   1)

  (matcho/match
   (sut/eval-data {:namespaces {'sty {'case {:a {:b 1}}}}}
                  {:id '(get-in sty/case [:a :b])
                   :symbol 'sty/string?})

   {:id 1})



  )
