(ns app.pages)

;; pages provide reg-page function,
;; which allows you to register page under
;; some keyword, which will be used as routing key

(defonce pages (atom {}))

(defn reg-page
  "register page under keyword for routing"
  ;;[key f & [layout-key]]
  [key page]
  (swap! pages assoc key page))
