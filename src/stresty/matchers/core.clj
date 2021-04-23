(ns stresty.matchers.core
  (:require [stresty.matchers.matcho]))

(defmulti match (fn [ztx matcher sample] (or (:type matcher) 'sty/matcho)))

(defmethod match :default
  [ztx matcher sample]
 {:errors [{:message (str (:type matcher) " is not implmented.")}]})

