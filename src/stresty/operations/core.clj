(ns stresty.operations.core
  (:require [zen.core :as zen]
            [stresty.actions.core :as acts]
            [stresty.matchers.core :as match]
            [stresty.sci]
            [stresty.format.core :as fmt]
            [clojure.string :as str]))
;; [clj-http.lite.client :as http]

(defmulti call-op (fn [ztx op args] (:zen/name op)))

(defmethod call-op :default
  [ztx op args]
  {:error {:message (str "Op " (:zen/name op) " is not implemented!")}})

(defmethod call-op 'sty/echo
  [ztx op {params :params}]
  {:result params})

(defn op [ztx args]
  (println ::op args)
  (if-let [op (when-let [m (:method args)]
                (zen/get-symbol ztx (symbol m)))]
    (call-op ztx op args)
    {:error {:message (str "Operation " (:method args) " is not defined.")}}))


(defn- get-case-state [ztx enm cnm]
  (or (get-in @ztx [:state enm cnm]) {}))

(defn- save-case-state [ztx enm cnm key state]
  (swap! ztx (fn [old] (update-in old [:state enm cnm] (fn [old] (assoc old key state))))))

(defn sty-url [& args]
  (str "/" (str/join  "/" args)))

(defn run-step [ztx {enm :zen/name :as env} {cnm :zen/name :as case} step-key {action :action :as step}]
  (let [state (get-case-state ztx enm cnm)
        action (stresty.sci/eval-data {:namespaces {'sty {'step step 'case case 'state state 'url sty-url}}} action)
        ev-base {:type 'sty/on-step-start :env env :case case
                 :step (assoc step :id step-key)
                 :verbose (get-in @ztx [:opts :verbose])
                 :action action}]
    (fmt/emit ztx ev-base)
    (try
      (let [result (stresty.actions.core/run-action ztx {:state state :case case :env env} action)]
        (save-case-state ztx enm cnm step-key result)
        (if-let [err (:error result)]
          (fmt/emit ztx (assoc ev-base :type 'sty/on-step-error :error err))
          (if-let [matcher (:matches step)]
            (let [errors (stresty.matchers.core/match
                          ztx
                          (stresty.sci/eval-data {:namespaces {'sty {'step step 'case case 'state state}}} matcher)
                          result)]
              (if-not (empty? errors)
                (fmt/emit ztx (assoc ev-base :type 'sty/on-step-fail :errors errors :result result :matcher matcher))
                (fmt/emit ztx (assoc ev-base :type 'sty/on-step-fail :errors errors :result result :matcher matcher))))
            (fmt/emit ztx (assoc ev-base :type 'sty/on-step-result :result result)))))
      (catch Exception e
        (fmt/emit ztx (assoc ev-base :type 'sty/on-step-exception :exception e))))))

(defn run-case [ztx env case]
  (fmt/emit ztx {:type 'sty/on-case-start :env env :case case})
  (doseq [[k step] (->> (:steps case)
                        (sort-by (fn [[_ x]] (:row (meta x)))))]
    (run-step ztx env case k step))
  (fmt/emit ztx {:type 'sty/on-case-end :env env :case case}))

(defn run-env [ztx env]
  (fmt/emit ztx {:type 'sty/on-env-start :env env})
  (doseq [case-ref (zen/get-tag ztx 'sty/case)]
    (let [case (zen/get-symbol ztx case-ref)]
      (run-case ztx env case)))
  (fmt/emit ztx {:type 'sty/on-env-end :env env}))

(defmethod call-op 'sty/run-tests
  [ztx op {params :params}]
  (doseq [env-ref (zen/get-tag ztx 'sty/env)]
    (let [env (zen/get-symbol ztx env-ref)]
      (run-env ztx env)))
  {:result params})
