(ns pprint
  (:require [colors]
            [clojure.string :as str]))

(defn- -pretty [{ident :ident errs :errors :as ctx} obj &[f]]
  (cond
    (map? obj)
    (loop [[[k v] & kvs] obj
           frst true]
      (print (apply str (repeat (if (and frst f) 0 ident) "  ")))
      (print (colors/dark ((if (get-in errs (into (:path ctx) [k]))
                             colors/red
                             colors/bold) (str (name k) ":"))) " ")
      (if (or (map? v) (sequential? v)) (print "\n"))
      (-pretty (-> (update ctx :ident inc)
                  (update :path into [k])) v)
      (when-not (empty? kvs)
        (println)
        (recur kvs false)))
    (sequential? obj)
    (loop [[i & is] obj
           idx 0
           frst true]
      (print (apply str (repeat (- ident 1) "  ")))
      (print (str "-"))
      (print " ")
      (if (sequential? i)
        (do
          (println)
          (-pretty (-> (update ctx :ident inc)
                      (update :path into [idx])) i))
        (-pretty (update ctx :path into [idx]) i true))
      (when-not (empty? is)
        (println)
        (recur is (inc idx) false)))
    :else (if-let [er (get-in errs (:path ctx))]
            (print (colors/bold (colors/red obj))  (colors/green "!= " (:expected er)))
            (print obj))))

(defn pretty [{ident :ident errs :errors :as ctx} obj &[f]]
  (-pretty ctx obj f)
  (println))

(comment

  (println "\n-------------\n")

  (println "\n")
  (pretty {:errors {:birthDate {:expected "yyy"}
                    :name {:given {:expected "ups"}}
                    :telecom {0 {:value "ups"}}}
           :path []
           :ident 0}
          {:birthDate "xxxxx" :number 33 :name {:given "given" :family "family"}
           :telecom [{:value "7483223" :system "sys"}
                     {:value "3223" :system "sys"}]})
  )

