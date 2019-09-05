(ns user (:require [cider-nrepl.main]))


(defn -main [& args]
  (-> (Thread/currentThread)
      (.setName "cider"))
  (cider-nrepl.main/init
   ["refactor-nrepl.middleware/wrap-refactor"
    "cider.nrepl/cider-middleware"]))
