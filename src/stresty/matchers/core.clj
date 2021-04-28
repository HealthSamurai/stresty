(ns stresty.matchers.core
  (:require [stresty.matchers.matcho]))

(defmulti match (fn [ztx matcher sample] (or (:by matcher) 'sty/matcho)))

(defmethod match :default
  [ztx matcher sample]
 {:errors [{:message (str (:type matcher) " is not implmented.")}]})

(defmethod match 'sty/matcho
  [ztx matcher sample]
  (let [result  (stresty.matchers.matcho/match ztx {} sample (dissoc matcher :by))]
    (when (seq result)
      {:errors result})))
