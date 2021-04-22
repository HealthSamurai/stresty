(ns stresty.operations)

(defmulti call-op (fn [ztx op args] (:zen/name op)))

(defmethod call-op :default
  [ztx op args]
  {:error {:message (str "Op " (:zen/name op) " is not implemented!")}})

(defmethod call-op 'sty/echo
  [ztx op {params :params}]
  {:result params})

