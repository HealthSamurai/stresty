(ns b64
  (:import java.util.Base64))

;; see https://stackoverflow.com/questions/11825444/clojure-base64-encoding

(defn encode [to-encode]
  (.encodeToString (Base64/getEncoder) (.getBytes to-encode)))

(defn decode [to-decode]
  (String. (.decode (Base64/getDecoder) to-decode)))
