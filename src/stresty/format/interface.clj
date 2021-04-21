(ns stresty.format.interface)

(defmulti format (fn [ztx fmt state event] fmt))
