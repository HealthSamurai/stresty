(ns stresty.server.cli-test
  (:require [stresty.server.cli :as sut]
            [matcho.core :as matcho]
            [zen.core :as zen]
            [clojure.test :as t]))


(t/deftest test-cli

  (def ztx (zen/new-context {}))
  (zen/read-ns ztx 'sty)

  (matcho/match
   (sut/parse-args ["--path=PATH" "tests" "--parama=1" "--paramb=2"])
   {:params {:path "PATH"},
    :command {:name "tests", :params {:parama "1", :paramb "2"}}})

  (matcho/match
   (sut/parse-args ["path:PATH" "tests" "parama:1" "paramb:some.ns,another.ns"])
   {:params {:path "PATH"},
    :command {:name "tests", :params {:parama "1", :paramb ["some.ns" "another.ns"]}}})

  (matcho/match
   (sut/cmd-index ztx)
   {"test" {}})

  (sut/parse-args ["tests"
                   "ns:mynamespace"
                   "tg:aidbox,grahame"
                   "cs:my.case-1,my.case-2"])

  (matcho/match
   (sut/resolve-cmd ztx {:name "ups"})
   {:error
    {:message #"Command <ups> is not found. Available commands .*"}})

  (matcho/match
   (sut/resolve-cmd ztx (:command (sut/parse-args ["test"
                                                   "e:aidbox"
                                                   "ns:mynamespace"
                                                   "tg:aidbox,grahame"
                                                   "cs:my.case-1,my.case-2"])))

   {:result {:name 'sty/cli-test
             :params {:tag ["aidbox" "grahame"]
                      :case ["my.case-1" "my.case-2"]
                      :env ["aidbox"]
                      :ns ["mynamespace"]}}})


  (println (sut/usage ztx))

  )
