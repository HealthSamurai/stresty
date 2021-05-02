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
                      {:params (get-in df [:params :keys])
                       :require (:require df)
                       :def df
                       :aliases (->> (get-in df [:params :keys])
                                     (reduce (fn [acc [k v]]
                                               (if-let [a (:cli/alias v)]
                                                 (assoc acc (keyword a) k)
                                                 acc))
                                             {}))}))
             ) {})))

(defn coerce-params [defs params]
  (->> params
       (reduce (fn [acc [k v]]
                 (let [df (get defs k)]
                   (cond
                     (and (= 'zen/vector (:type df)) (not (sequential? v)))
                     (assoc acc k [v])

                     (and (= 'zen/integer (:type df)))
                     (assoc acc k (Integer/parseInt v))

                     :else (assoc acc k v)))) {})))

(defn mk-params [{prms-df :params als :aliases :as df} params]
  (->> params
       (reduce
        (fn [acc [k v]]
          (cond
            (contains? prms-df k) (assoc acc k v)
            (contains? als k)     (assoc acc (als k) v)
            :else (assoc acc k v))) {})
       (coerce-params prms-df)))

(defn resolve-cmd [ztx {nm :name :as cmd}]
  (let [idx (cmd-index ztx)]
    (if-let [df (get idx nm)]
      (let [prms (mk-params df (:params cmd))
            {errs :errors}  (zen/validate-schema ztx (get-in df [:def :params]) prms)]
        (if (empty? errs)
          {:result {:name (get-in df [:def :zen/name])
                    :definition df
                    :params prms}}
          {:error {:errors errs
                   :params prms
                   :message "Invalid params"}}))
      {:error {:message (format "Command <%s> is not found. Available commands %s"
                                nm (str/join ", "(map name (keys idx))))}})))

(defn param-short-desc [ztx p-nm {tp :type :as p-df}]
  (str (name p-nm) "="
       (if-let [ex (:cli/example p-df)]
         ex
         (if (= 'zen/vector tp)
           (let [etp (get-in p-df [:every :type])]
             (str "<" (name etp) "," (name etp) ">"))
           (str "<" (name tp)">")))))

(defn param-desc [ztx p-nm p-df]
  (str "  " (param-short-desc ztx p-nm p-df) "\t-\t" (:zen/desc p-df)))



(defn cmd-usage [ztx cmd df]
  (str "sty " cmd
       " "
       (->> (:params df)
            (map (fn [[p-nm p-df]]
                   (let [p (param-short-desc ztx p-nm p-df)]
                     (if (contains? (:require df) p-nm)
                       p
                       (str "[" p "]")))))
            (str/join " "))
       "\n "(get-in df [:def :zen/desc])
       "\n params:\n"
       (->> (:params df)
            (map (fn [[p-nm p-df]]
                   (param-desc ztx p-nm p-df)))
            (str/join "\n")
            )))

(defn usage [ztx]
  (let [idx (cmd-index ztx)]
    (str 
     "======================================================\n\n"
     "sty " (str/join " | " (keys idx)) "\n\n"
     "------------------------------------------------------"
     "\n"
     (->> idx
          (mapv (fn [[cmd df]] (cmd-usage ztx cmd df)))
          (str/join "\n\n-------------------------------------------------\n")))))
