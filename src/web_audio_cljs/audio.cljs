(ns web-audio-cljs.audio
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [web-audio-cljs.utils :refer [l]]))

(defn clear-canvas! [canvas-context canvas-width canvas-height bg-color]
  (if (nil? bg-color)
    (.clearRect canvas-context 0 0 canvas-width canvas-height)
    (do
      (set! (.-fillStyle canvas-context) bg-color)
      (.fillRect canvas-context 0 0 canvas-width canvas-height)
      (set! (.-fillStyle canvas-context) "#F6D565")
      (set! (.-lineCap canvas-context) "round"))))

(defn draw-line-on-canvas!
  [canvas-context canvas-height i spacing num-bars bar-width magnitude]
  (set! (.-fillStyle canvas-context)
        (str "hsl(" (string/join "," [(.round js/Math (/ (* i 360) num-bars)) "100%" "50%"]) ")"))
  (.fillRect canvas-context (* i spacing) canvas-height bar-width (- magnitude)))

(defn get-time-domain-data [analyser-node num-bars]
  (let [Uint8Array (.-Uint8Array js/window)
        freq-bin-count (.-frequencyBinCount analyser-node)]
    {:freq-byte-data (Uint8Array. freq-bin-count)
     :multiplier (/ (.-frequencyBinCount analyser-node) num-bars)}))

;;  page 52 of web-audio book
;;  get teh getByteFrequenceData and just take the max value of the data.
(defn draw-bars!
  [canvas-context canvas-width canvas-height spacing num-bars multiplier freq-byte-data bar-width]
  (clear-canvas! canvas-context canvas-width canvas-height "#000000")
  (doseq [i (range num-bars)]
    (let [offset (.floor js/Math (* i multiplier))
          magnitude (/ (reduce #(+ (aget freq-byte-data (+ offset %2)) %1) (range multiplier))
                       multiplier)]
      (draw-line-on-canvas! canvas-context canvas-height i spacing num-bars bar-width magnitude))))

(defn max-of-array [array-of-nums]
  (.apply js/Math.max nil array-of-nums))
(defn min-of-array [array-of-nums]
  (.apply js/Math.min nil array-of-nums))

(defn draw-circle! [canvas freq-byte-data n]
  (let [canvas-context (.getContext canvas "2d")
        canvas-width (.-width canvas)
        canvas-height (.-height canvas)
        max-val (max-of-array freq-byte-data)
        r (* (/ canvas-width 2) (/ max-val 256))
        center-x (/ canvas-width 2)
        center-y center-x]
    (clear-canvas! canvas-context canvas-width canvas-height nil)
    (.beginPath canvas-context)
    (.arc canvas-context center-x center-y r 0 (* 2 (.-PI js/Math)) false)
    (aset canvas-context "fillStyle" "red")
    (.fill canvas-context)))
