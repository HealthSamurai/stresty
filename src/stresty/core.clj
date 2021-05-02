(ns stresty.core
  (:require [stresty.server.core :as server])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -main [& args]
  (let [{err :error} (stresty.server.core/main args)]
    (if err
      (System/exit 1)
      (System/exit 0))))
