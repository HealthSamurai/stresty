(ns app.hack.interop
  #?(:cljs (:require [cljsjs.js-yaml :as yaml])))

(defn stringify-key-preserve-ns [k]
  (clojure.string/join "/" (remove nil? [(namespace k) (name k)])))

(defn to-json [x & [inline]]
  #?(:cljs (if inline
             (js/JSON.stringify (clj->js x :keyword-fn stringify-key-preserve-ns))
             (js/JSON.stringify (clj->js x :keyword-fn stringify-key-preserve-ns) nil " "))))

(defn to-yaml [x]
  #?(:cljs (js/jsyaml.safeDump (clj->js x :keyword-fn stringify-key-preserve-ns))))
