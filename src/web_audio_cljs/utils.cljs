(ns web-audio-cljs.utils
  (:require [clojure.string :as string]
            [cljs.core.async :refer [put! chan]]
            [goog.events :as events]))

(defn l [& args] (.log js/console " " (string/join args)))

(defn recording-duration [bpm]
  "Length of a quarter note in milliseconds."
  (* (/ bpm 60) 1000))

(defn recording-duration-sec [bpm]
  "Length of a quarter note in seconds"
  (/ bpm 60))

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
