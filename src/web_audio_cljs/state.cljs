(ns web-audio-cljs.state
  (:require [om.core :as om :include-macros true]
            [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [<!]]
            [cljs.core.match :refer-macros [match]]
            [web-audio-cljs.utils :refer [lin-interp]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(def wave-width 400)
(def wave-height 100)
(def track-width 600)
(def track-sample-height 40)
(def sample-width 80)
(def sample-height 80)
(def bpm 120)
(def composition-duration-sec 10)
(def seconds-per-beat (/ bpm 60))
(def sixteenth-note-length (* 0.25 seconds-per-beat))
(def audio-look-ahead-time-sec 0.5)

(defn recording-duration []
  "Length of a quarter note in milliseconds."
  (* (/ bpm 60) 1000))

(defn recording-duration-sec []
  "Length of a quarter note in seconds"
  (/ bpm 60))

(def note-type->num
  {"Eighth" 8 "Quarter" 4 "Half" 2 "Whole" 1})

(def note-type->width
  {"Eighth"  (/ wave-width 8)
   "Quarter" (/ wave-width 4)
   "Half"    (/ wave-width 2)
   "Whole"   wave-width})

(def note-types (keys note-type->width))

(def note-type->bg-color
  {"Eighth"  "mediumvioletred"
   "Quarter" "darkorchid"
   "Half"    "darksalmon"
   "Whole"   "peachpuff"})

(defn by-id [cursor id]
  (first (filter #(= (:id %) id) cursor)))

(declare samples)

(defn track-sample->bg-color [track-sample]
  (let [sample (by-id (samples) (:sample track-sample))]
    (get note-type->bg-color (:type sample))))

(def note-type->color
  {"Eighth"  "blanchedalmond"
   "Quarter" "blanchedalmond"
   "Half"    "blanchedalmond"
   "Whole"   "mediumpurple"})

(let [db {:compositions []
          :tracks []
          :track-samples []
          :samples []
          :sounds []
          :analyser-node nil
          :audio-recorder nil
          :is-recording false
          :ui {:buffers-visible true
               :selected-track nil
               :is-playing false}}]

  (defonce app-state (atom db)))

(defonce audio-context (js/window.AudioContext.))

(defn samples []
  (om/ref-cursor (:samples (om/root-cursor app-state))))

(defn sounds []
  (om/ref-cursor (:sounds (om/root-cursor app-state))))

(defn tracks []
  (om/ref-cursor (:tracks (om/root-cursor app-state))))

(defn track-samples []
  (om/ref-cursor (:track-samples (om/root-cursor app-state))))

(defn ui []
  (om/ref-cursor (:ui (om/root-cursor app-state))))

(defn make-new-sound [audio-buffer sound-name]
  {:id (uuid/make-random)
   :name sound-name
   :audio-buffer audio-buffer
   :current-offset 0
   :current-note-type "Quarter"
   :editing-name false})

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
   :is-playing false
   :offset 0})

(defn save-sound! [app-state sound-name]
  (let [{:keys [audio-recorder analyser-node]} @app-state
        buffer-length (.-frequencyBinCount analyser-node)]
    (.stop audio-recorder)
    (.getBuffers audio-recorder
                 (fn [buffers]
                   (om/transact! app-state :sounds
                                 #(conj % (make-new-sound (aget buffers 0) sound-name)))
                   (.clear audio-recorder)))))

(defn track-samples-for-track [track]
  (let [track-samples (track-samples)]
    (map #(by-id track-samples %) (:track-samples track))))

(defn sample-duration [sample]
  (let [sample-width (get note-type->width (:type sample))]
    (* (/ sample-width wave-width) (recording-duration-sec))))

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

(defn play-track-sample! [track-sample]
  (let [sample (by-id (samples) (:sample track-sample))]
    (play-buffer! audio-context
                  (:audio-buffer sample)
                  ((lin-interp 0 wave-width 0 (recording-duration-sec)) (:offset sample))
                  (sample-duration sample))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-toggle-recording [app-state sound-name]
  (let [{:keys [is-recording audio-recorder]} @app-state]
    (if is-recording
      (save-sound! app-state sound-name)
      (.record audio-recorder))
    (om/transact! app-state :is-recording not)))

(defn handle-update-sound-note-type [sound note-type]
  (let [current-selector-width (note-type->width note-type wave-width)
        x-offset (.clamp goog.math (:current-offset sound) 0 (- wave-width current-selector-width))]
    (om/transact! sound #(assoc % :current-note-type note-type :current-offset x-offset))))

(defn handle-add-sample-to-track [sample]
  (when-let [track (:selected-track (ui))]
    (let [new-t-sample (make-track-sample sample)
          new-track-samples (conj (:track-samples track) (:id new-t-sample))
          new-track (assoc track :track-samples new-track-samples)]
      (om/transact! track (fn [_] new-track))
      (om/transact! (ui) #(assoc % :selected-track new-track))
      (om/transact! (track-samples) #(conj % new-t-sample)))))

(defn start-actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
    (match [action-vec]
      [[:toggle-recording sound-name]] (handle-toggle-recording app-state sound-name)
      [[:set-sound-note-type sound note-type]] (handle-update-sound-note-type sound note-type)
      [[:toggle-sound-name-edit sound]] (om/transact! sound #(assoc % :editing-name (not (:editing-name %))))
      [[:set-sound-offset sound x-offset]] (om/transact! sound #(assoc % :current-offset x-offset))
      [[:set-sound-name sound new-name]] (om/transact! sound #(assoc % :name new-name))
      [[:new-sample sound]] (om/transact! (samples) #(conj % (make-new-sample sound)))
      [[:make-new-track]]  (om/transact! (tracks) #(conj % (make-new-track)))
      [[:set-track-name track track-name]] (om/transact! track #(assoc % :name track-name))
      [[:toggle-buffers]] (om/transact! (ui) :buffers-visible not)
      [[:add-sample-to-track sample]] (handle-add-sample-to-track sample)
      [[:select-track track]] (om/transact! (ui) #(assoc % :selected-track track))
      [[:set-track-sample-offset track-sample offset]] (om/transact! track-sample #(assoc % :offset offset))
      [[:toggle-playback]] (om/transact! (ui) :is-playing not)
      [[:play-track-samples track-samples-to-play]]
           (doseq [s track-samples-to-play] (play-track-sample! s))
      :else (.error js/console "Unknown handler: " (clj->js action-vec)))
    (recur (<! actions-chan))))
