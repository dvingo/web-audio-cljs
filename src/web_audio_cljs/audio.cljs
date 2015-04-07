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


(def canvas (.getElementById js/document "display"))
(def canvas-context (.getContext canvas "2d"))
(def canvas-width (.-width canvas))
(def canvas-height (.-height canvas))
(def spacing 3)
(def bar-width 1)
(def num-bars (.round js/Math (/ canvas-width spacing)))

(defn clear-canvas! [canvas-context]
  (set! (.-fillStyle canvas-context) "#000000")
  (.fillRect canvas-context 0 0 canvas-width canvas-height)
  (set! (.-fillStyle canvas-context) "#F6D565")
  (set! (.-lineCap canvas-context) "round"))

(defn draw-line-on-canvas!
  [canvas-context i spacing num-bars bar-width magnitude]
  (set! (.-fillStyle canvas-context)
        (str "hsl(" (string/join "," [(.round js/Math (/ (* i 360) num-bars)) "100%" "50%"]) ")"))
  (.fillRect canvas-context (* i spacing) canvas-height bar-width (- magnitude)))

(defn chart-view [{:keys [analyser-node] :as data} owner]
  (reify
    om/IDisplayName (display-name [_] "chart")

    om/IInitState
    (init-state [_]
      (let [Uint8Array (.-Uint8Array js/window)
            freq-bin-count (.-frequencyBinCount analyser-node)
            freq-byte-data (Uint8Array. freq-bin-count)
            multiplier (/ (.-frequencyBinCount analyser-node) num-bars)]
        {:freq-byte-data freq-byte-data
         :multiplier multiplier}))

    om/IDidMount
    (did-mount [_]
      (let [Uint8Array (.-Uint8Array js/window)
            freq-bin-count (.-frequencyBinCount analyser-node)
            freq-byte-data (Uint8Array. freq-bin-count)
            multiplier (/ (.-frequencyBinCount analyser-node) num-bars)]
        (om/set-state! owner { :freq-byte-data freq-byte-data :multiplier multiplier})))

    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (let [Uint8Array (.-Uint8Array js/window)
            freq-bin-count (.-frequencyBinCount analyser-node)
            freq-byte-data (Uint8Array. freq-bin-count)
            multiplier (/ (.-frequencyBinCount analyser-node) num-bars)]
        (om/set-state! owner { :freq-byte-data freq-byte-data :multiplier multiplier})))

    om/IRenderState
    (render-state [_ {:keys [freq-byte-data multiplier]}]
      (.getByteFrequencyData analyser-node freq-byte-data)
      (clear-canvas! canvas-context)
      (doseq [i (range num-bars)]
        (let [offset (.floor js/Math (* i multiplier))
              magnitude (/ (reduce #(+ (aget freq-byte-data (+ offset %2)) %1) (range multiplier))
                           multiplier)]
          (draw-line-on-canvas! canvas-context i spacing num-bars bar-width magnitude))))))
