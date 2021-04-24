(ns app.hack.interop)

;; (defn to-yaml [x]
;;   #?(:clj  (yaml/generate-string x)
;;      :cljs (yaml/safeDump (clj->js x) #js {:indent 2 :lineWidth 300})))



;; (defn from-yaml [x]
;;   #?(:clj  (yaml/parse-string x)
;;      :cljs (js->clj (yaml/safeLoad x) :keywordize-keys true)))

(defn stringify-key-preserve-ns [k]
  (clojure.string/join "/" (remove nil? [(namespace k) (name k)])))

(defn to-json [x & [inline]]
  #?(:cljs (if inline
             (js/JSON.stringify (clj->js x :keyword-fn stringify-key-preserve-ns))
             (js/JSON.stringify (clj->js x :keyword-fn stringify-key-preserve-ns) nil " "))))

(defn to-pretty-edn [x]
  (with-out-str #?(:cljs (cljs.pprint/pprint x)
                   :clj (clojure.pprint/pprint x))))




