(ns chrono.locale-ru
  (:require [chrono.util :as util]))

(defmethod util/locale :ru [_]
  {:month
   {1 {:name "Январь", :short "Янв", :regex "(?iu)янв(ар(ь|я))?"}
    2 {:name "Февраль", :short "Фев", :regex "(?iu)фев(рал(ь|я))?"}
    3 {:name "Март", :short "Мар", :regex "(?iu)мар(та?)?"}
    4 {:name "Апрель", :short "Апр", :regex "(?iu)апр(ел(ь|я)?)?"}
    5 {:name "Май", :short "Май", :regex "(?iu)ма(й|я)?"}
    6 {:name "Июнь", :short "Июн", :regex "(?iu)июн(ь|я)?"}
    7 {:name "Июль", :short "Июл", :regex "(?iu)июл(ь|я)?"}
    8 {:name "Август", :short "Авг", :regex "(?iu)авг(уста?)?"}
    9 {:name "Сентябрь", :short "Сен", :regex "(?iu)сен(тябр(ь|я)?)?"}
    10 {:name "Октябрь", :short "Окт", :regex "(?iu)окт(ябр(ь|я)?)?"}
    11 {:name "Ноябрь", :short "Ноя", :regex "(?iu)ноя(бр(ь|я)?)?"}
    12 {:name "Декабрь", :short "Дек", :regex "(?iu)дек(бр(ь|я)?)?"}}})
