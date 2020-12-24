(ns app.yaml
  (:require
   #?(:clj  [clj-yaml.core :as yaml]
      :cljs ["js-yaml" :as yaml])))

(defn to-yaml [x]
  #?(:clj  (yaml/generate-string x)
     :cljs (yaml/safeDump (clj->js x))))


(defn from-yaml [x]
  #?(:clj  (yaml/parse-string x)
     :cljs (js->clj (yaml/safeLoad x) :keywordize-keys true)))

(defn stringify-key-preserve-ns [k]
  (clojure.string/join "/" (remove nil? [(namespace k) (name k)])))

(defn to-json [x & [inline]]
  #?(:cljs (if inline
             (js/JSON.stringify (clj->js x :keyword-fn stringify-key-preserve-ns))
             (js/JSON.stringify (clj->js x :keyword-fn stringify-key-preserve-ns) nil " "))))
