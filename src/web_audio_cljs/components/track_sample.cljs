(ns web-audio-cljs.components.track-sample
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.utils :refer [lin-interp listen]]
            [web-audio-cljs.state :refer [sample-from-id]])
  (:import [goog.events EventType])
  (:require-macros [web-audio-cljs.macros :refer [send!]]
                   [cljs.core.async.macros :refer [go go-loop alt!]]))

(def track-width 300)

(defn rel-mouse-x-pos [mouse-x {:keys [x-offset mouse-down-x]}]
  (let [x-delta (- mouse-x mouse-down-x)]
    [(+ x-offset x-delta) mouse-x]))

(defn track-sample-view [t-sample owner]
  (reify
    om/IDisplayName (display-name [_] "track-sample-view")
    om/IInitState
    (init-state [_]
      {:mouse-down false
       :mouse-down-x 0
       :x-offset 0
       :t-sample-width 40})

    om/IDidMount
    (did-mount [_]
      (let [{:keys [x-offset t-sample-width]} (om/get-state owner)
            max-x-pos (- track-width t-sample-width)
            mouse-move-chan (listen js/document (.-MOUSEMOVE EventType)
                                    (comp (filter #(:mouse-down (om/get-state owner)))
                                          (map #(rel-mouse-x-pos (.-clientX %)
                                                                 (om/get-state owner)))
                                          (map (fn [[new-x mouse-x]]
                                                 [(.clamp goog.math new-x 0 max-x-pos) mouse-x]))))
            mouse-up-chan (listen js/document (.-MOUSEUP EventType))]
        (go-loop []
          (alt!
            mouse-move-chan ([[new-x mouse-down-x]]
              (send! owner :set-track-sample-offset t-sample new-x)
              (om/update-state! owner #(assoc % :x-offset new-x :mouse-down-x mouse-down-x)))
            mouse-up-chan ([_]
              (om/set-state! owner :mouse-down false)))
          (recur))))

    om/IRenderState
    (render-state [_ {:keys [x-offset]}]
      (let [offset-str  (str "translate("x-offset"px,0px)")]
        (.log js/console "x-offset: " x-offset)
        (.log js/console "x-offset-str: " offset-str)
        (dom/div #js {:className "track-sample"
                      :style #js {:WebkitTransform offset-str}
                      :onMouseDown
                      (fn [e] (om/update-state! owner
                         #(assoc % :mouse-down true :mouse-down-pos [(.-clientX e) (.-clientY e)])))}
           nil)))))
