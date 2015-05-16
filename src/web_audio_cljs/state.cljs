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

(def note-type->num
  {"Eighth" 8 "Quarter" 4 "Half" 2 "Whole" 1})

(def note-type->width
  {"Eighth"  (/ wave-width 8)
   "Quarter" (/ wave-width 4)
   "Half"    (/ wave-width 2)
   "Whole"   wave-width})

(def note-types (keys note-type->width))

(def note-type->bg-color
  {"Eighth" "mediumvioletred"
   "Quarter" "darkorchid"
   "Half" "darksalmon"
   "Whole" "peachpuff"})

(declare sample-from-id)

(defn track-sample->bg-color [track-sample]
  (let [s (:sample track-sample)
        sample (sample-from-id s)]
    (.log js/console "s ID: " s)
    (.log js/console "sample FROM ID: " sample)

    (get note-type->bg-color (:type sample))))

(def note-type->color
  {"Eighth" "blanchedalmond"
   "Quarter" "blanchedalmond"
   "Half" "blanchedalmond"
   "Whole" "mediumpurple"})

(let [db {:compositions []
          :tracks []
          :samples []
          :sounds []
          :analyser-node nil
          :audio-recorder nil
          :is-recording false
          :bpm 120
          :ui {:buffers-visible true}}]

  (defonce app-state (atom db)))

(defonce audio-context (js/window.AudioContext.))

(defn samples []
  (om/ref-cursor (:samples (om/root-cursor app-state))))

(defn sounds []
  (om/ref-cursor (:sounds (om/root-cursor app-state))))

(defn tracks []
  (om/ref-cursor (:tracks (om/root-cursor app-state))))

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

(defn make-new-track []
  {:id (uuid/make-random)
   :name nil
   :track-samples []})

(defn make-track-sample [sample]
  {:id (uuid/make-random)
   :sample (:id sample)
   :offset 0})

(defn sample-from-id [sample-id]
  (first (filter #(= (:id %) sample-id) (samples))))

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

(defn handle-set-track-name [app-state track trk-name]
  (let [i (last (om/path track))
        new-track (assoc (get (tracks) i) :name trk-name)]
    (om/transact! (tracks) #(assoc % i new-track))))

(defn handle-add-sample-to-track [app-state sample track]
  (let [new-track-samples (conj (:track-samples track) (make-track-sample sample))
        new-track (assoc track :track-samples new-track-samples)
        i (last (om/path track))]
  (om/transact! (tracks) #(assoc % i new-track))))

(defn start-actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
    (match [action-vec]
      [[:toggle-recording sound-name]] (handle-toggle-recording app-state sound-name)
      [[:set-sound-note-type sound note-type]]
           (handle-update-sound-note-type app-state sound note-type)
      [[:set-sound-offset sound-index x-offset]]
           (handle-update-sound-offset sound-index x-offset)
      [[:new-sample sound]] (om/transact! (samples) #(conj % (make-new-sample sound)))
      [[:make-new-track]]  (om/transact! (tracks) #(conj % (make-new-track)))
      [[:set-track-name track trk-name]] (handle-set-track-name app-state track trk-name)
      [[:toggle-buffers]] (om/transact! app-state [:ui :buffers-visible] not)
      [[:add-sample-to-track sample]] (handle-add-sample-to-track app-state sample (first (tracks)))
      :else (.error js/console "Unknown handler: " (clj->js action-vec)))
    (recur (<! actions-chan))))
