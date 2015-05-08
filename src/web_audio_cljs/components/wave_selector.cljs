(ns web-audio-cljs.components.wave-selector
  (:require [cljs.core.async :refer [>! put!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.utils :refer [listen]]
            [web-audio-cljs.state :refer [wave-width]])
  (:import [goog.events EventType])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn rel-mouse-pos [e {:keys [x-offset mouse-down-pos]}]
  (let [x (.-clientX e)
        [old-x old-y] mouse-down-pos
        new-x (+ x-offset (- x old-x))]
    [new-x x old-x old-y]))

(defn draw-select-rect! [canvas-width canvas-height canvas-context mouse-down? mouse-over?]
  (.clearRect canvas-context 0 0 canvas-width canvas-height)
  (.fillRect canvas-context 0 0 canvas-width canvas-height)
  (cond
    mouse-down?
    (do
      (aset canvas-context "strokeStyle" "aliceblue")
      (aset canvas-context "lineWidth" 6)
      (.strokeRect canvas-context 2 2 (- canvas-width 4) (- canvas-height 4)))
    mouse-over?
    (do
      (aset canvas-context "strokeStyle" "aliceblue")
      (aset canvas-context "lineWidth" 2)
      (.strokeRect canvas-context 0 0 canvas-width canvas-height))))

(defn wave-selector-view [recorded-sound owner]
  (reify
    om/IDisplayName (display-name [_] "wave-selector-view")

    om/IInitState
    (init-state [_] {:canvas nil
                     :canvas-context nil
                     :canvas-height 100
                     :mouse-down false
                     :mouse-down-pos []
                     :mouse-over false})

    om/IDidMount
    (did-mount [_]
      (let [{:keys [canvas-height left-x]} (om/get-state owner)
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")
            mouse-move-chan (listen js/document (.-MOUSEMOVE EventType)
                                    (comp (filter #(:mouse-down (om/get-state owner)))
                                          (map #(rel-mouse-pos % (om/get-state owner)))))
            mouse-up-chan (listen js/document (.-MOUSEUP EventType))]
        (go-loop []
          (alt!
            mouse-move-chan
            ([[new-x mouse-down-x _ old-y]]
             (let [clamp-x
                   (.clamp goog.math new-x 0 (- wave-width (:canvas-width (om/get-state owner))))]
             (>! (:action-chan (om/get-shared owner))
                 [:set-recorded-sound-offset (last (om/path recorded-sound)) clamp-x])
             (om/update-state! owner #(assoc % :x-offset clamp-x :mouse-down-pos [mouse-down-x old-y]))))
            mouse-up-chan
            ([_] (om/set-state! owner :mouse-down false)))
          (recur))
        (om/update-state! owner #(assoc % :canvas-context canvas-context :canvas canvas))))

    om/IDidUpdate
    (did-update [_ _ _]
      (let [{:keys [canvas-height canvas-context mouse-down
                    mouse-over canvas-width]} (om/get-state owner)]
        (if canvas-context
          (draw-select-rect! canvas-width canvas-height canvas-context mouse-down mouse-over)
          (om/refresh! owner))))

    om/IRenderState
    (render-state [_ {:keys [canvas-height mouse-down x-offset canvas-width max-width]}]
      (dom/canvas #js {:width       canvas-width
                       :height      canvas-height
                       :style       #js {:opacity 0.3
                                         :position "absolute"
                                         :left x-offset
                                         :cursor (if mouse-down "move" "default")}
                       :ref         "canvas-ref"
                       :onMouseDown (fn [e] (om/update-state! owner
                                              #(assoc % :mouse-down true
                                                        :mouse-down-pos [(.-clientX e) (.-clientY e)])))
                       :onMouseOver #(om/set-state! owner :mouse-over true)
                       :onMouseOut  #(om/set-state! owner :mouse-over false)}
                  "no canvas"))))
