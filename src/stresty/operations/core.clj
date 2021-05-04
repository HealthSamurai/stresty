(ns stresty.operations.core
  (:require [zen.core :as zen]
            [stresty.actions.core :as acts]
            [stresty.matchers.core :as match]
            [stresty.sci]
            [stresty.format.core :as fmt]
            [stresty.operations.gen]
            [stresty.reports.core]
            [clojure.string :as str]))

(defmulti call-op (fn [ztx op args] (:zen/name op)))

(defn op [ztx args]
  (if-let [op (when-let [m (:method args)]
                (zen/get-symbol ztx (symbol m)))]
    (call-op ztx op args)
    (do
      (println (str "Operation " (:method args) " is not defined."))
      {:error {:message (str "Operation " (:method args) " is not defined.")}})))


(defmethod call-op :default
  [ztx op args]
  (println (str "Op " (:zen/name op) " is not implemented!"))
  {:error {:message (str "Op " (:zen/name op) " is not implemented!")}})

(defmethod call-op 'sty/echo
  [ztx op {params :params}]
  {:result params})

(defmethod call-op 'sty/get-namespaces
  [ztx op _]
  (let [cases (zen/get-tag ztx 'sty/case)]
    {:result {:namespaces (group-by (fn [e] (first (str/split (str e) #"\/"))) cases)}}))

(defmethod call-op 'sty/get-case
  [ztx op {params :params}]
  (if-let [case (zen/get-symbol ztx (symbol (:case params)))]
    {:result case}
    {:error {:message "Case not found"}}))

(defn- get-case-state [ztx enm cnm]
  (or (get-in @ztx [:state enm cnm]) {}))

(defn- save-case-state [ztx enm cnm key state]
  (swap! ztx (fn [old] (update-in old [:state enm cnm] (fn [old] (assoc old key state))))))

(defn- save-step-result [ztx cnm {idx :_index :as step} result]
  (swap! ztx update :result #(assoc-in % [cnm idx] result)))

(defn sty-url [& args]
  (str "/" (str/join  "/" args)))

(defn run-step [ztx {enm :zen/name :as env} {cnm :zen/name :as case} {id :id idx :_index action :do :as step}]

  (fmt/emit ztx (assoc step :type 'sty/on-run-step))

  (let [state (get-case-state ztx enm cnm)
        action (stresty.sci/eval-data {:namespaces {'sty {'env env 'step step 'case case 'state state 'url sty-url}}} action)
        ev-base {:type 'sty/on-step-start
                 :env env
                 :case case
                 :step (assoc step :id (or id idx))
                 :verbose (get-in @ztx [:opts :verbose])
                 :do action}]
    (fmt/emit ztx ev-base)
    (try
      (let [{result :result error :error} (stresty.actions.core/run-action ztx {:state state :case case :env env} action)]
        (fmt/emit ztx (assoc ev-base :type 'sty/on-action-result :result result :error error))
        (if error
          (do
            (save-step-result ztx cnm step {:status :error :error error})
            (fmt/emit ztx (assoc ev-base :type 'sty/on-step-error :error error))
            (assoc step :status :error :error error))
          (do
            (when id (save-case-state ztx enm cnm id result))
            (if-let [matcher (:match step)]
              (let [*matcher (stresty.sci/eval-data {:namespaces {'sty {'env env 'step step 'case case 'state state}}} matcher)
                    {errors :errors} (stresty.matchers.core/match ztx *matcher result)]
                (if (empty? errors)
                  (do
                    (save-step-result ztx cnm step {:status :success})
                    (fmt/emit ztx (assoc ev-base :type 'sty/on-match-ok :result result :matcher matcher))
                    (assoc step :status :ok :result result))

                  (do
                   (save-step-result ztx cnm step {:status :error :error error :result result})
                   (fmt/emit ztx (assoc ev-base :type 'sty/on-match-fail :errors errors :result result :matcher matcher))
                   (assoc step :status :fail :match-errors errors :result result))))
              (do (fmt/emit ztx (assoc ev-base :type 'sty/on-match-ok))
                  (assoc step :status :ok :result result))))))
      (catch Exception e
        (fmt/emit ztx (assoc ev-base :type 'sty/on-step-error :error {:message (.getMessage e)}))
        (assoc step :status :error :error {:message (.getMessage e)})))))

(defn run-case [ztx env case]
  (fmt/emit ztx {:type 'sty/on-case-start :env env :case case})
  (let [res (->> (:steps case)
                 (map-indexed (fn [idx step]
                                (let [step (assoc step :_index idx)]
                                  (run-step ztx env case step))))
                 (into []))]
    (fmt/emit ztx {:type 'sty/on-case-end :env env :case case :result res})
    {:case case :steps res}))

(defn run-env [ztx env]
  (fmt/emit ztx {:type 'sty/on-env-start :env env})
  (let [cases (->> (zen/get-tag ztx 'sty/case)
                   (mapv (fn [case-ref] (zen/get-symbol ztx case-ref))))
        result (->> cases
                    (reduce (fn [res case]
                              (assoc res (:zen/name case) (run-case ztx env case)))
                            {}))]
    (fmt/emit ztx {:type 'sty/on-env-end :env env :result result})
    {:cases result :env env}))

(defmethod call-op 'sty/run-tests
  [ztx op {params :params}]
  (fmt/set-formatter ztx (:format params))
  (fmt/emit ztx {:type 'sty/on-tests-start})
  (let [envs-filter (when (:env params) (into #{} (mapv symbol (:env params))))
        envs (->> (zen/get-tag ztx 'sty/env)
                  (filter (fn [env-ref] (or (nil? envs-filter) (contains? envs-filter env-ref))))
                  (map (fn [env-ref]
                         (zen/get-symbol ztx env-ref))))
        result (->> envs
                    (reduce (fn [report env]
                              (let [res (run-env ztx env)]
                                (assoc report (:zen/name env) res))) {}))]
    (stresty.reports.core/build-report ztx params result)
    (fmt/emit ztx {:type 'sty/on-tests-done :result result})
    result))

(defmethod call-op 'sty/gen
  [ztx op {params :params}]
  (println "Generate" params)
  (stresty.operations.gen/generate ztx params))
