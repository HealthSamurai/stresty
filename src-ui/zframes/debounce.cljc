(ns zframes.debounce
  (:require
    [zframes.re-frame :as zrf]))


(defn now []
  #?(:cljs (.getTime (js/Date.))))


(def registered-keys (atom nil))


(defn dispatch-if-not-superseded [{:keys [key event time-received]}]
  (when (= time-received (get-in @registered-keys [key :time-received]))
    (swap! registered-keys dissoc key)
    (zrf/dispatch event)))


(zrf/defe :dispatch-debounce
  [debounce]
  #?(:clj (zrf/dispatch (:event debounce))
     :cljs (let [debounce (assoc debounce :time-received (now))
                 timout-fn (fn [] (dispatch-if-not-superseded debounce))]
             (swap! registered-keys assoc (:key debounce)
                    {:time-received (:time-received debounce)
                     :timeout-fn timout-fn})
             (js/setTimeout timout-fn (:delay debounce 300)))))


(zrf/defe :dispatch-debounce/flush
  [key]
  #?(:cljs (when-let [timeout-fn (:timeout-fn (get @registered-keys key))]
             (timeout-fn))))


(zrf/defe :dispatch-debounce/clear
  [key]
  #?(:cljs (swap! registered-keys dissoc key)))
