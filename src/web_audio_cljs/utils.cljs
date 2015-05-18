(ns web-audio-cljs.utils
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [cljs.core.async :refer [put! chan]]
            [web-audio-cljs.state :refer [by-id samples wave-width note-type->width bpm]]
            [goog.events :as events]))

(defn l [& args] (.log js/console " " (string/join args)))

(defn recording-duration [bpm]
  "Length of a quarter note in milliseconds."
  (* (/ bpm 60) 1000))

(defn recording-duration-sec [bpm]
  "Length of a quarter note in seconds"
  (/ bpm 60))

(defn sample-duration [sample]
  (let [sample-width (get note-type->width (:type sample))]
    (* (/ sample-width wave-width) (recording-duration-sec bpm))))

(defn play-buffer! [audio-context buffer-data offset duration]
  (let [source (.createBufferSource audio-context)
        buffer (.createBuffer audio-context 1
                              (.-length buffer-data)
                              (.-sampleRate audio-context))
        chan-data (.getChannelData buffer 0)]
    (.set chan-data buffer-data)
    (aset source "buffer" buffer)
    (.connect source (.-destination audio-context))
    (.start source 0 offset duration)))

(declare lin-interp)
(defn play-track-sample! [audio-context track-sample]
  (let [sample (by-id (samples) (:sample track-sample))
        recording-length (recording-duration-sec bpm)]
    (.log js/console "playing sample: " (:name sample))
    (.log js/console ": ")
    (play-buffer! audio-context
                  (:audio-buffer sample)
                  ((lin-interp 0 wave-width 0 recording-length) (:offset sample))
                  (sample-duration sample))))

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
