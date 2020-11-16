(ns stresty.core
  (:require [zen.core :as zen]
            [clojure.string :as str]))


(def ctx (zen/new-context))

(defn no-errors! [ctx]
  (when-not (empty? (:errors @ctx))
    (println (str/join "\n" (mapv pr-str (:errors @ctx))))
    (assert (empty? (:errors @ctx)) "See STDOUT for errors")))

(zen/read-ns ctx 'stresty)

(no-errors! ctx)

(zen/read-ns ctx 'stresty.tests.core)

(no-errors! ctx)


(doseq [case-id (zen/get-tag ctx 'stresty/case)]
  (let [case (zen/get-symbol ctx case-id)]
    (println "Run case\n" (:desc case))
    (doseq [stp (:steps case)]
      (println "* step" stp))))


(->
 (zen/validate
  ctx #{'stresty/case}
  {:desco "Wrong name"
   :steps
   [{:GET 1
     :headers "string"
     :match {:status "200"
             :body {:id 'stresty/unknown?}}}]})
 (:errors)
 (->> (str/join "\n"))
 (println))

