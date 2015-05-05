(ns web-audio-cljs.utils
  (:require [clojure.string :as string]
            [cljs.core.async :refer [put! chan]]
            [goog.events :as events]))

(defn l [& args] (.log js/console " " (string/join args)))

(defn note-type->width [note-type max-width]
  (case (str note-type)
    "eighth" (/ max-width 8)
    ":eighth" (/ max-width 8)
    "quarter" (/ max-width 4)
    ":quarter" (/ max-width 4)
    "half" (/ max-width 2)
    ":half" (/ max-width 2)
    "whole" max-width
    ":whole" max-width))

(defn recording-duration [bpm]
  "Length of a quarter note in milliseconds."
  (* (/ bpm 60) 1000))

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
