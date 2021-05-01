(ns stresty.server.cli
  (:require [clojure.string :as str]
            [zen.core :as zen]))


(defn parse-args [args]
  (loop [[a & as] args
         params {}]
    (if (and (nil? a) (empty? as))
      params
      (if (or (str/starts-with? a "-") (str/includes? a ":") (str/includes? a "="))
        (let [[k v] (str/split a #"(=|:)" 2)]
          (recur as (assoc-in params [:params (keyword (str/replace k #"^--?" ""))]
                              (if (str/includes? v ",")
                                (-> (str/trim v)
                                    (str/split #",")
                                    (into []))
                                (str/trim v)
                                ))))
        (assoc params :command (merge {:name (str/trim a)} (parse-args as)))))))

(defn cmd-index [ztx]
  (->> 
   (zen/get-tag ztx 'sty/cli-cmd)
   (mapv #(zen/get-symbol ztx %))
   (reduce (fn [acc {nm :cli/name :as df}]
             (if-not nm
               acc
               (assoc acc nm
                      {:params (:params df)
                       :def df
                       :aliases (->> (get-in df [:params :keys])
                                     (reduce (fn [acc [k v]]
                                               (if-let [a (:cli/alias v)]
                                                 (assoc acc (keyword a) k)
                                                 acc))
                                             {}))}))
             ) {})))

(defn resolve-cmd [ztx {nm :name :as cmd}]
  (let [idx (cmd-index ztx)]
    (if-let [df (get idx nm)]
      {:result {:cmd (get-in df [:def :zen/name])
                :params {:cmd (:params cmd) :df (:params df)} :def df}}
      {:error {:message (format "Command <%s> is not found. Available commands %s"
                                nm (str/join ", "(map name (keys idx))))}})))
