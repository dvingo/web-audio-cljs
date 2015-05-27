(ns web-audio-cljs.utils
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [cljs.core.async :refer [put! chan]]
            [goog.events :as events]))

(defn l [& args] (.log js/console " " (string/join args)))

(defn listen
  ([el type]
   (let [out (chan)]
     (events/listen el type
       (fn [e] (put! out e)))
     out))
  ([el type tx]
   (let [out (chan 1 tx)]
     (events/listen el type
       (fn [e] (put! out e)))
     out)))

(defn lin-interp [x0 x1 y0 y1]
  (fn [x]
    (+ y0
       (* (/ (- x x0)
             (- x1 x0))
          (- y1 y0)))))

(defn superlative-of [arr compar]
  (loop [i 0 min-val (aget arr 0)]
    (if (= i (.-length arr))
      min-val
      (let [cur-val (aget arr i)]
        (if (apply compar [cur-val min-val])
          (recur (inc i) cur-val)
          (recur (inc i) min-val))))))

(defn min-arr-val [arr]
  (superlative-of arr <))

(defn max-arr-val [arr]
  (superlative-of arr >))

(defn max-of-array [array-of-nums]
  (.apply js/Math.max nil array-of-nums))

(defn min-of-array [array-of-nums]
  (.apply js/Math.min nil array-of-nums))

(defn set-prop-if-undefined! [prop obj options]
  (when-not (aget obj prop)
    (let [opts (map #(aget obj %) options)
          prop-to-use (first (filter #(not (nil? %)) opts))]
      (aset obj prop prop-to-use))))

(defn make-button [disp-name on-click btn-label]
  (fn [data owner]
    (reify
      om/IDisplayName (display-name [_] disp-name)
      om/IRender
      (render [_] (dom/button #js {:onClick on-click} btn-label)))))

(defn get-time-domain-data-with-bars [analyser-node num-bars]
  (let [Uint8Array (.-Uint8Array js/window)
        freq-bin-count (.-frequencyBinCount analyser-node)]
    {:freq-byte-data (Uint8Array. freq-bin-count)
     :multiplier (/ (.-frequencyBinCount analyser-node) num-bars)}))

(defn get-time-domain-data [analyser-node]
  (let [Uint8Array (.-Uint8Array js/window)
        freq-bin-count (.-frequencyBinCount analyser-node)]
    (Uint8Array. freq-bin-count)))

(defn clear-canvas! [canvas-context canvas-width canvas-height bg-color]
  (if (nil? bg-color)
    (.clearRect canvas-context 0 0 canvas-width canvas-height)
    (do
      (set! (.-fillStyle canvas-context) bg-color)
      (.fillRect canvas-context 0 0 canvas-width canvas-height)
      (set! (.-fillStyle canvas-context) "#F6D565")
      (set! (.-lineCap canvas-context) "round"))))

(defn classes [& args]
  (string/join " " args))
