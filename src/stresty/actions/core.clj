(ns stresty.actions.core)

(defmulti run-action (fn [ztx action args] (:zen/name action)))

(defmethod run-action :default
  [ztx action args]
  {:error {:message (str "Action " (:zen/name action) " is not implemented!")}})
