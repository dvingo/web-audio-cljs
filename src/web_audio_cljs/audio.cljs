(ns web-audio-cljs.audio
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [web-audio-cljs.utils :refer [l]]
            ))

;; Browser BS dance.
(defn set-prop-if-undefined! [prop obj options]
  (when-not (aget obj prop)
    (let [opts (map #(aget obj %) options)
          prop-to-use (first (filter #(not (nil? %)) opts))]
      (aset obj prop prop-to-use))))
(set-prop-if-undefined! "AudioContext" js/window ["AudioContext" "webkitAudioContext"])
(set-prop-if-undefined! "getUserMedia" js/navigator ["webkitGetUserMedia" "mozGetUserMedia"])
(set-prop-if-undefined! "cancelAnimationFrame" js/window
                        ["webkitCancelAnimationFrame" "mozCancelAnimationFrame"])
(set-prop-if-undefined! "requestAnimationFrame" js/window
                        ["webkitRequestAnimationFrame" "mozRequestAnimationFrame"])

(defn clear-canvas! [canvas-context canvas-width canvas-height]
  (set! (.-fillStyle canvas-context) "#000000")
  (.fillRect canvas-context 0 0 canvas-width canvas-height)
  (set! (.-fillStyle canvas-context) "#F6D565")
  (set! (.-lineCap canvas-context) "round"))

(defn draw-line-on-canvas!
  [canvas-context canvas-height i spacing num-bars bar-width magnitude]
  (set! (.-fillStyle canvas-context)
        (str "hsl(" (string/join "," [(.round js/Math (/ (* i 360) num-bars)) "100%" "50%"]) ")"))
  (.fillRect canvas-context (* i spacing) canvas-height bar-width (- magnitude)))

(defn get-audio-data [analyser-node num-bars]
  (let [Uint8Array (.-Uint8Array js/window)
        freq-bin-count (.-frequencyBinCount analyser-node)]
    {:freq-byte-data (Uint8Array. freq-bin-count)
     :multiplier (/ (.-frequencyBinCount analyser-node) num-bars)}))

(defn draw-bars!
  [canvas-context canvas-width canvas-height spacing num-bars multiplier freq-byte-data bar-width]
  (clear-canvas! canvas-context canvas-width canvas-height)
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
        {:canvas nil
         :canvas-context nil
         :canvas-width nil
         :canvas-height nil
         :spacing 3
         :bar-width 1
         :num-bars nil
         :freq-byte-data nil
         :multiplier nil})

    om/IDidMount
    (did-mount [_]
      (let [{:keys [spacing]} (om/get-state owner)
            canvas (.getElementById js/document "display")
            canvas-context (.getContext canvas "2d")
            canvas-width (.-width canvas)
            canvas-height (.-height canvas)
            num-bars (.round js/Math (/ canvas-width spacing))
            {:keys [freq-byte-data multiplier]} (get-audio-data analyser-node num-bars)]
        (om/update-state! owner #(assoc %
                                        :canvas canvas
                                        :canvas-context canvas-context
                                        :canvas-width canvas-width
                                        :canvas-height canvas-height
                                        :num-bars num-bars
                                        :freq-byte-data freq-byte-data
                                        :multiplier multiplier))))

    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (let [{:keys [canvas-context canvas-width canvas-height num-bars spacing bar-width]} (om/get-state owner)
            {:keys [freq-byte-data multiplier]} (get-audio-data analyser-node num-bars)]
        (.getByteFrequencyData analyser-node freq-byte-data)
        (when canvas-context
          (draw-bars! canvas-context canvas-width canvas-height spacing
                      num-bars multiplier freq-byte-data bar-width))
        (om/update-state! owner #(assoc % :freq-byte-data freq-byte-data :multiplier multiplier))))

    om/IRender
    (render [_] nil)))
