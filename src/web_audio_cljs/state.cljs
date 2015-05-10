(ns web-audio-cljs.state
  (:require [om.core :as om :include-macros true]
            [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [put! chan timeout <! >! onto-chan]]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def wave-width 400)
(def wave-height 100)
(def sample-width 80)
(def sample-height 80)

(def note-type->width
  {"Eighth"  (/ wave-width 8)
   "Quarter" (/ wave-width 4)
   "Half"    (/ wave-width 2)
   "Whole"   wave-width})
(def note-types (keys note-type->width))

(def note-type->bg-color
  {"Eighth" "midnightblue"
   "Quarter" "darkorchid"
   "Half" "darksalmon"
   "Whole" "peachpuff"})

(def note-type->color
  {"Eighth" "blanchedalmond"
   "Quarter" "blanchedalmond"
   "Half" "blanchedalmond"
   "Whole" "mediumpurple"})

(def note-type->arc
  {"Eighth" "M .5 1 A .5 .5, 0, 0, 0, .75 .95 L .5 .5 Z"
   "Quarter" "M .5 1 A .5 .5, 0, 0, 0, 1 .5 L .5 .5 Z"
   "Half" "M .5 1 A .5 .5, 0, 0, 0, .5 0 Z"})

(def note-type->rotate-path
  {"Eighth" "rotate(-150 .5 .5)"
   "Quarter" "rotate(-90 .5  .5)"
   "Half" ""})

(let [db {:compositions []
          :tracks []
          :sounds []
          :samples []
          :analyser-node nil
          :audio-recorder nil
          :is-recording false
          :bpm 120}]

  (defonce app-state (atom db)))

(defonce audio-context (js/window.AudioContext.))

(defn samples []
  (om/ref-cursor (:samples (om/root-cursor app-state))))

(defn sounds []
  (om/ref-cursor (:sounds (om/root-cursor app-state))))

(defn make-new-sound [audio-buffer sound-name]
  {:id (uuid/make-random)
   :name sound-name
   :audio-buffer audio-buffer
   :current-offset 0
   :current-note-type "Quarter"})

(defn save-sound! [app-state sound-name]
  (let [{:keys [audio-recorder analyser-node]} @app-state
        buffer-length (.-frequencyBinCount analyser-node)]
    (.stop audio-recorder)
    (.getBuffers audio-recorder
                 (fn [buffers]
                   (om/transact! app-state :sounds
                                 #(conj % (make-new-sound (aget buffers 0) sound-name)))
                   (.clear audio-recorder)))))

(defn make-new-sample [sound]
  {:id (uuid/make-random)
   :name (:name sound)
   :audio-buffer (:audio-buffer sound)
   :offset (:current-offset sound)
   :type (:current-note-type sound)
   :sound (:id sound)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-toggle-recording [app-state sound-name]
  (let [{:keys [is-recording audio-recorder]} @app-state]
    (if is-recording
      (save-sound! app-state sound-name)
      (.record audio-recorder))
    (om/transact! app-state :is-recording not)))

(defn handle-update-sound-note-type [app-state sound note-type]
  (let [i (last (om/path sound))
        current-selector-width (note-type->width note-type wave-width)
        x-offset (.clamp goog.math (:current-offset sound) 0 (- wave-width current-selector-width))
        new-sound (assoc sound :current-note-type note-type :current-offset x-offset)]
    (om/transact! (sounds) #(assoc % i new-sound))))

(defn handle-update-sound-offset [sound-idx x-offset]
  (let [snds (sounds)
        new-sound (assoc (get snds sound-idx) :current-offset x-offset)]
    (om/transact! snds #(assoc % sound-idx new-sound))))

(defn handle-new-sample [app-state sound]
  (om/transact! (samples) #(conj % (make-new-sample sound))))

(defn start-actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
     (match [action-vec]
       [[:toggle-recording sound-name]] (handle-toggle-recording app-state sound-name)
       [[:set-sound-note-type sound note-type]]
            (handle-update-sound-note-type app-state sound note-type)
       [[:set-sound-offset sound-index x-offset]]
            (handle-update-sound-offset sound-index x-offset)
       [[:new-sample sound]] (handle-new-sample app-state sound)
       :else (.log js/console "Unknown handler: " (clj->js action-vec)))
     (recur (<! actions-chan))))
