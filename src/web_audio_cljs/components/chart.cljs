(ns web-audio-cljs.components.chart
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [web-audio-cljs.utils :refer [max-of-array min-of-array get-time-domain-data-with-bars
                                          clear-canvas!]]))

(defn draw-line-on-canvas!
  [canvas-context canvas-height i spacing num-bars bar-width magnitude]
  (set! (.-fillStyle canvas-context)
        (str "hsl(" (string/join "," [(.round js/Math (/ (* i 360) num-bars)) "100%" "50%"]) ")"))
  (.fillRect canvas-context (* i spacing) canvas-height bar-width (- magnitude)))

(defn draw-bars!
  [canvas-context canvas-width canvas-height spacing num-bars multiplier freq-byte-data bar-width]
  (clear-canvas! canvas-context canvas-width canvas-height "#000000")
  (doseq [i (range num-bars)]
    (let [offset (.floor js/Math (* i multiplier))
          magnitude (/ (reduce #(+ (aget freq-byte-data (+ offset %2)) %1) (range multiplier))
                       multiplier)]
      (draw-line-on-canvas! canvas-context canvas-height i spacing num-bars bar-width magnitude))))


(defn chart-view [{:keys [analyser-node] :as data} owner]
  (reify
    om/IDisplayName (display-name [_] "chart")

    om/IInitState
    (init-state [_]
      {:bars-canvas nil
       :bars-canvas-context nil
       :spacing 3
       :bar-width 1
       :num-bars nil
       :freq-byte-data nil
       :multiplier nil})

    om/IDidMount
    (did-mount [_]
      (let [{:keys [spacing]} (om/get-state owner)
            bars-canvas (om/get-node owner "bars-canvas-ref")
            num-bars (.round js/Math (/ (.-width bars-canvas) spacing))
            {:keys [freq-byte-data multiplier]} (get-time-domain-data-with-bars analyser-node num-bars)]
        (om/update-state! owner #(assoc % :bars-canvas bars-canvas
                                          :bars-canvas-context (.getContext bars-canvas "2d")
                                          :num-bars num-bars
                                          :freq-byte-data freq-byte-data
                                          :multiplier multiplier))))

    om/IDidUpdate
    (did-update [_ _ _]
      (let [{:keys [bars-canvas bars-canvas-context circle-canvas circle-canvas-context
                    num-bars spacing bar-width]} (om/get-state owner)
            {:keys [freq-byte-data multiplier]} (get-time-domain-data-with-bars analyser-node num-bars)]
        (.getByteFrequencyData analyser-node freq-byte-data)
        (when bars-canvas-context
          (draw-bars! bars-canvas-context (.-width bars-canvas) (.-height bars-canvas) spacing
                      num-bars multiplier freq-byte-data bar-width))
        (om/update-state! owner #(assoc % :freq-byte-data freq-byte-data :multiplier multiplier))))

    om/IRender
    (render [_]
      (dom/canvas #js {:width  500
                       :height 80
                       :className "bars-chart"
                       :ref    "bars-canvas-ref"} "no canvas"))))
