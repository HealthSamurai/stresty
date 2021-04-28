(ns stresty.operations.gen-test
  (:require [stresty.operations.gen :as sut]
            [zen.core :as zen]
            [clojure.test :as t]
            [clojure.java.io :as io]))


(t/deftest test-generator
  (def ztx (zen/new-context {:paths [".tmp/wdir"]}))

  (sut/path ".tmp" ["wdir"])

  (sut/rmdir (sut/path ".tmp" ["wdir"]))
  (t/is (not (sut/exists? (sut/path ".tmp" ["wdir"]))))

  (sut/generate ztx {:project "testproj"})

  (t/is (.exists (io/file ".tmp/wdir/envs.edn")))
  (t/is (.exists (io/file ".tmp/wdir/testproj/case.edn")))



  )

