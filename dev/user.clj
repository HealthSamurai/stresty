(ns user
  (:require
    [clojure.java.io :as io]
    [clojure.tools.namespace.repl :as repl]
    [shadow.cljs.devtools.api :as shadow]
    [shadow.cljs.devtools.config :as shadow.config]
    [shadow.cljs.devtools.server :as shadow.server]
    [stresty.web.core]))

(defn delete-recursively [f]
  (when (.isDirectory f)
    (doseq [c (.listFiles f)]
      (delete-recursively c)))
  (io/delete-file f))

(defn restart [ctx]
  (stresty.web.core/restart ctx))

(defn restart-shadow-clean []
  (shadow.server/stop!)
  (try (-> (shadow.config/get-build :app)
           (get-in [:dev :output-dir])
           (io/file)
           (delete-recursively))
       (delete-recursively (io/file ".shadow-cljs"))
       (catch Exception _))
  (shadow.server/start!)
  (shadow/watch :app))

(defn restart-ui []
  (restart-shadow-clean))

(defn reload-ns []
  (repl/set-refresh-dirs "src" "src-ui" "src-c")
  (repl/refresh))

(comment
  
  (reload-ns)

  (do
    (defonce *context (atom {}))
    (def ztx (zen.core/new-context))
    (swap! *context assoc :ztx ztx)

    (zen.core/read-ns ztx 'user)
    (zen.core/read-ns ztx 'config))
  
  (restart *context)

  (restart-ui)
  
  )

