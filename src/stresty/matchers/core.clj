(ns stresty.matchers.core
  (:require [stresty.matchers.matcho]))

(defmulti match (fn [ztx matcher sample pattern] (or (:zen/name matcher) 'sty/matcho)))

(defmethod match :default
  [ztx matcher sample pattern]
 {:errors [{:message (str (pr-str matcher) " is not implmented.")}]})

