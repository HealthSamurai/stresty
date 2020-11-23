(ns zframes.hotkeys
  (:require [clojure.string :as str]
            [zframes.re-frame :as zrf]))


(def KEYS
  (merge {"backspace" 8,
          "tab"       9,
          "enter"     13, "return" 13,
          "pause"     19,
          "caps"      20, "capslock" 20,
          "escape"    27, "esc" 27,
          "space"     32,
          "pgup"      33, "pageup" 33,
          "pgdown"    34, "pagedown" 34,
          "end"       35,
          "home"      36,
          "ins"       45, "insert" 45,
          "del"       46, "delete" 46,

          "left"      37,
          "up"        38,
          "right"     39,
          "down"      40,

          "*"         106,
          "+"         107, "plus" 107, "kpplus" 107,
          "kpminus"   109,
          ";"         186,
          "="         187,
          ","         188,
          "-"         189, "minus" 189,
          "."         190,
          "/"         191,
          "`"         192,
          "["         219,
          "\\"        220,
          "]"         221,
          "'"         222}


         ;; numpad
         (into {} (for [i (range 10)]
                    [(str "num-" i) (+ 95 i)]))

         ;; top row 0-9
         (into {} (for [i (range 10)]
                    [(str i) (+ 48 i)]))

         ;; f1-f24
         (into {} (for [i (range 1 25)]
                    [(str "f" i) (+ 111 i)]))

         ;; alphabet
         ;;a-z
         (into {} (for [i (range 65 91)]
                    [(char i) i]))
         ;;A-Z
         (into {} (for [i (range 97 123)]
                    [(char i) (+ 203 i)]))))

         

(def ^:private known-keys
  (into {} (for [[k v] KEYS]
             [v k])))

;; Data

(defonce bindings (atom {}))
(defonce pressed (atom []))
(defonce enabled? (atom false))


(defn parse-button [button]
  (let [code (get KEYS button)]
    (when-not code
      (throw (ex-info (str "Unknown key '" button "'") {})))
    code))

(defn parse [chain]
  (let [buttons (.split chain " ")]
    (mapv parse-button buttons)))

(def letters #{\a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z})

(defn e->code [e]
  (if (or (not (letters (-> e .-keyCode char str/lower-case))) (.-shiftKey e))
    (.-keyCode e)
    (+ 235 (.-keyCode e))))

(defn reset-sequence! []
  (swap! pressed empty)
  (reset! enabled? false))

(defn dispatch [e bindings]
  (let [code     (e->code e)
        sequence (conj @pressed code)
        inner    (get-in bindings sequence)
        handlers (:handlers inner)]
    (cond
      (not inner) (reset-sequence!)
      handlers (do
                 (.preventDefault e)
                 (.stopPropagation e)
                 (doseq [[_ handler] (:handlers inner)]
                   (handler e sequence))
                 (reset-sequence!))
      :else (do
              (.preventDefault e)
              (.stopPropagation e)
              (reset! pressed sequence)))))

(defn bind [bindings spec key cb]
  "Same as `bind!`, just modifies `bindings` map, you have to handle
  storage (like an atom) yourself."
  (let [parsed (parse spec)]
    (assoc-in bindings (conj parsed :handlers key) cb)))

(defn unbind [bindings spec key]
  "Same as `unbind!`, just modifies `bindings` map, you have to handle
  storage (like an atom) yourself."
  (let [parsed (parse spec)]
    (update-in bindings (conj parsed :handlers) dissoc key)))

(defn bind! [spec key cb]
  "Binds a sequence of button presses, specified by `spec`, to `cb` when
  pressed. Keys must be unique per `spec`, and can be used to remove keybinding
  with `unbind!`.
  `spec` format is emacs-like strings a-la \"ctrl-c k\", \"meta-shift-k\", etc."
  (swap! bindings bind spec key cb))

(defn unbind! [spec key]
  "Removes a callback, identified by `key`, from button sequence `spec`."
  (swap! bindings unbind spec key))

(defn unbind-all! []
  "Remove all BINDINGS"
  (reset-sequence!)
  (swap! bindings empty))

(defn disable! []
  "Disable dispatching of key events (but leave existing bindings intact)."
  (reset! enabled? false))

(defn enable! []
  "Enable dispatching of key events via the existing bindings."
  (reset! enabled? true))

(defn dispatcher! [bindings]
  "Return a function to be bound on `keydown` event, preferably globally.
  Accepts atom with bindings.
  Is bound by default with `keycode/BINDINGS` atom, so you don't need to use
  that."
  (fn [e]
    (when (or (= "Alt" (.-key e)) (and (not @enabled?) (.-altKey e)))
      (reset! enabled? true)
      (swap! pressed empty))
    (when (and @enabled? (get known-keys (.-keyCode e)))
      (dispatch e @bindings))))

;; Global key listener

(defonce bind-keypress-listener
  #?(:cljs (js/addEventListener "keydown" (dispatcher! bindings) false)
     :clj  nil))


(defonce bindings-history (atom nil))
(defonce bindings-tree (atom nil))


(defn make-bindings-tree
  [bindings-tree bindings-hiccup]
  #?(:cljs (reduce
             (fn [acc [button attrs & children]]
               (let [code (parse-button button)]
                 (assoc acc code (merge attrs
                                        {:button   button
                                         :children (make-bindings-tree
                                                     (get-in acc [code :children])
                                                     children)}))))
             bindings-tree
             bindings-hiccup)))


(defn flatten-bindings-tree
  [bindings-tree]
  (->> (vals bindings-tree)
       (mapcat (fn [{:keys [id button event children]}]
                 (if (seq children)
                   (->> (flatten-bindings-tree children)
                        (map #(update % 0 (partial str button " "))))
                   [[button id event]])))))


(zrf/defe :keybind/bind
  [key-bindings]
  (swap! bindings-history conj key-bindings)
  (reset! bindings-tree (reduce make-bindings-tree {} @bindings-history))

  (reset! bindings {})
  (doseq [[key id event] (flatten-bindings-tree @bindings-tree)]
    (bind! key id #(zrf/dispatch event))))


(zrf/defe :keybind/unbind
  [key-bindings]
  (swap! bindings-history (fn [history] (filterv #(not= % key-bindings) history)))
  (reset! bindings-tree (reduce make-bindings-tree {} @bindings-history))

  #?(:cljs (reset! bindings {}))
  (doseq [[key id event] (flatten-bindings-tree @bindings-tree)]
    #?(:cljs (bind! key id #(zrf/dispatch event)))))


(defn get-in-chord
  [keybindings pressed]
  (loop [keybindings keybindings
         [code & pressed] pressed]
    (if (nil? code)
      keybindings
      (recur (get-in keybindings [:children code])
             pressed))))
