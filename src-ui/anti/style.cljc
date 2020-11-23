(ns anti.style
  (:require
   [garden.core :as garden]
   [garden.units :as u]))


(def background-colors
  {:light        {:value "#ffffff"
                  :hover "#fafafa"}
   :dark         {:value "#2c3645"}
   :primary      {:value "#1890ff"
                  :hover "#40a9ff"}
   :gray         {:value "#f0f2f58c"}
   :selection    {:value "#e6f7ff"}
   :warn         {:value "#fffbe6"}

   :danger-light {:value "#ffa39e"}
   :danger       {:value "#ff4d4f"}
   :success      {:value "#b7eb8f"}})

(def border-colors
  {:default       "#d9d9d9"
   :thin          "#f0f0f0"
   :danger-light  "#ffa39e"
   :danger        "#ff4d4f"
   :active        "#40a9ff"})

(def shadow-colors
  {:default       "rgba(0,0,0,.15)"
   :active        "rgba(24,144,255,.4)"
   :danger        "rgb(254, 78, 80,.9)"})

(def border-color              "#d9d9d9")
(def thin-border-color         "#f0f0f0")
(def danger-light-border-color "#ffa39e")
(def danger-border-color       "#ff4d4f")
(def active-border-color       "#40a9ff")


(def light-background-color         "#ffffff")
(def dark-background-color          "#2c3645")
(def gray-background-color          "#f0f2f58c")
(def selection-background-color     "#e6f7ff")
(def hover-background-color         "#fafafa")
(def danger-light-background-color  "#ffa39e")
(def danger-background-color        "#ff4d4f")
(def warn-background-color          "#ffe58f")
(def success-background-color       "#b7eb8f")
(def primary-background-color       "#1890ff")
(def primary-hover-background-color "#40a9ff")


(def active-text-color  "#1890ff")
(def danger-text-color  "#ff4d4f")
(def success-text-color "#52c41a")
(def warn-text-color    "#faad14")
(def text-color         "rgba(0,0,0,.65)")
(def light-text-color   "rgba(0,0,0,.45)")

(def shadow-color        "rgba(0,0,0,.15)")
(def active-shadow-color "rgba(24,144,255,.4)")
(def danger-shadow-color "rgb(254, 78, 80,.9)")

(def pad {:padding-left "1em" :padding-right "1em"})
(def icon-height 16)
(def vertical-rythm 16)
(def vertical-rythm-l (* 2 vertical-rythm))
(def radius 3)
(def font-size (u/px 14))
(def pointer {:cursor "pointer"})
(def disabled {:pointer-events "none"})
(def no-outline {:outline "none"})

(def text {:color text-color
           :font-size font-size})

(def with-border {:border-color border-color
                  :border-width (u/px 1)
                  :border-style "solid"})
(def with-danger-border {:border-color danger-border-color})
(def with-active-border {:border-color active-border-color})

(def with-underline-border {:border-bottom-color border-color
                            :border-bottom-width (u/px 1)
                            :border-bottom-style "solid"})

(def with-overline-border {:border-top-color border-color
                           :border-top-width (u/px 1)
                           :border-top-style "solid"})

(def border-radius {:border-radius (u/px radius)})

(def with-light-background         {:background-color light-background-color})
(def with-danger-background        {:background-color danger-background-color})
(def with-selection-background     {:background-color selection-background-color})
(def with-hover-background         {:background-color hover-background-color})
(def with-primary-background       {:background-color primary-background-color})
(def with-primary-hover-background {:background-color primary-hover-background-color})
(def with-gray-background          {:background-color gray-background-color})

(def with-regular-text    {:color text-color})
(def with-active-text     {:color active-text-color})
(def with-danger-text     {:color danger-text-color})
(def with-light-text      {:color light-text-color})
(def with-white-text      {:color "white"})

(def with-shadow {:box-shadow (str "0 0 2px " shadow-color)})
(def with-active-shadow {:box-shadow (str "0 0 2px " active-shadow-color)})
(def with-danger-shadow {:box-shadow (str "0 0 2px " danger-shadow-color)})

(def inline-block
  {:height (u/px vertical-rythm-l)
   :line-height (u/px vertical-rythm-l)
   :font-size (u/px font-size)
   :box-sizing "border-box"
   :display "flex"
   :align-items "baseline"})

(def inline-block-with-border
  (merge inline-block
         with-border
         border-radius
         {:line-height (u/px (- vertical-rythm-l 2))}))


(def dropdown
  {:position         "absolute"
   :left             0
   ;;:right            0
   :top              "100%"
   :margin-top       "4px"
   :z-index          2
   :background-color "white"
   :box-shadow       "0 3px 6px -4px rgba(0,0,0,.12), 0 6px 16px 0 rgba(0,0,0,.08), 0 9px 28px 8px rgba(0,0,0,.05)"})

(def styles
  (concat
   (for [[mod {clr :value hover :hover}] background-colors]
     (cond-> [(keyword (str "." (name mod) "-background")) {:background-color clr}]
       hover (conj [:&:hover {:background-color hover}])))

   (for [[mod clr] border-colors]
     [(keyword (str "." (name mod) "-border")) {:border-color clr
                                                :border-width "1px"
                                                :border-style "solid"}])

   (for [style #{"padding" "margin"}
         direction #{"left" "right" "top" "bottom"}
         [size px] {"xl" 32 "l" 16 "" 8 "s" 4 "xs" 2}]
     [(keyword (str "." (first style) (first direction) (when (seq size) (str "-" size))))
      {(keyword (str style "-" direction)) (str px "px")}])


   (for [style #{"padding" "margin"}
         [type direction] {"h" ["left" "right"] "y" ["top" "bottom"]}
         [size px] {"xl" 32 "l" 16 "" 8 "s" 4 "xs" 2}]
     [(keyword (str "." (first style) type (when (seq size) (str "-" size))))
      (into {} (map #(vector (keyword (str style "-" %)) (str px "px")) direction))])

   [[:.line {:display "flex"
             :flex-direction "row"
             :flex-wrap "wrap"
             :margin-bottom (u/px 8)
             :align-content "space-between"}
     #_[:.inline-block {:margin-top (u/px 4) :margin-bottom (u/px 4)}]]
    [:.inline-block inline-block]
    [:.mr {:margin-right "1em"}]
    [:.pad pad]

    [:.btn (merge inline-block-with-border
                  with-light-background
                  pointer
                  pad)
     [:&:hover (merge with-active-border
                      with-active-text)]
     [:&:focus (merge with-active-shadow
                      with-active-border
                      with-active-text)]]

    [:.btn-danger (merge inline-block-with-border
                         with-danger-border
                         with-light-background
                         with-danger-text
                         pointer
                         pad)
     [:&:hover (merge with-danger-background
                      with-white-text)]
     [:&:focus (merge with-danger-background
                      with-white-text
                      with-danger-shadow)]]

    [:.btn-primary (merge inline-block-with-border
                          with-primary-background
                          with-white-text
                          with-active-border
                          pointer
                          pad)
     [:&:hover with-primary-hover-background]
     [:&:focus (merge with-primary-hover-background
                      with-active-shadow)]]

    [:.disabled (merge with-gray-background
                       with-border
                       with-light-text
                       disabled)]

    [:input.anti-input {:line-height "32px"
                        :box-sizing "border-box"
                        :align-items "baseline"
                        :color "rgba(0,0,0,.65)"
                        :font-size "14px"
                        :transition "all .3s"
                        :background-color "white"
                        :padding-right "8px"
                        :padding-left "8px"
                        :outline "none"
                        :display "flex"
                        :border "1px solid #d9d9d9"
                        :border-radius "3px"
                        :height "32px"}
     [:&:focus :&.focus {:color text-color
                         :border-color "#40a9ff"
                         :outline "none"
                         :box-shadow "0 0 2px 2px #1890ff52"}]]

    [:.with-sidebar
     [:.sidebar
      {:position "absolute"
       :top 0
       :bottom 0
       :left 0
       :background-color gray-background-color
       :box-shadow "0px 4px 7px #d0d0d0"
       :z-index "1000"
       :width "300px"
       :margin-right "10px"}
      [:.section
       {:display "block"
        :opacity "0.7"
        :text-decoration "none"
        :color "black"
        :font-size "14px"
        :border-bottom "1px solid #ddd"
        :font-weight "400"
        :border-left "3px solid transparent"
        :padding "8px 14px"}
       [:&:hover {:color "#1890ff"
                  :opacity 0.9}]
       [:&.active {:border-left "3px solid #1890ff"
                   :opacity "1"
                   :font-weight "500"
                   :color "#1890ff"}]]]
     [:.content
      {:position "absolute"
       :left "300px"
       :padding "20px"
       :background-color "white"
       :overflow "auto"
       :top 0
       :bottom 0
       :right 0}]]]))
