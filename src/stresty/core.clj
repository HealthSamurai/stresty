(ns stresty.core
  (:require [zen.core :as zen]
            [stresty.web.core :as server]))

(defonce *context (atom {:ztx (zen.core/new-context)}))

(defn -main [& args]
  (prn "starting server... ")
  (server/start *context)

  )
