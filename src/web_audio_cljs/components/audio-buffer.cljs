(ns web-audio-cljs.components.audio-buffer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put!]]
            [web-audio-cljs.components.wave-selector :refer [wave-selector-view]]
            [web-audio-cljs.utils :refer [l min-arr-val max-arr-val lin-interp note-type->width]]))

(defn draw-buffer! [width height canvas-context data]
  (let [step (.ceil js/Math (/ (.-length data) width))
        amp (/ height 2)
        min-val (min-arr-val data)
        max-val (max-arr-val data)
        scale (lin-interp min-val max-val (- amp) amp)]
    (aset canvas-context "fillStyle" "silver")
    (.clearRect canvas-context 0 0 width height)
    (doseq [i (range width)]
      (doseq [j (range step)]
        (let [datum (.min js/Math (aget data (+ (* i step) j)) 1)
              height (scale datum)]
          (.fillRect canvas-context i amp 1 height))))))

(defn play-buffer [audio-context buffer-data]
  (let [source (.createBufferSource audio-context)
        buffer (.createBuffer audio-context 1
                              (.-length buffer-data)
                              (.-sampleRate audio-context))
        chan-data (.getChannelData buffer 0)]
    (.set chan-data buffer-data)
    (aset source "buffer" buffer)
    (.connect source (.-destination audio-context))
    (.start source 0)))

(defn play-audio-buffer-view [{:keys [buffer-data audio-context]} owner]
    (reify
    om/IDisplayName (display-name [_] "play-audio-buffer-view")
    om/IRender
    (render [_]
      (dom/button #js {:onClick #(play-buffer audio-context buffer-data)} "Play"))))

(defn note-type-view [recorded-sound owner]
  (reify
    om/IDisplayName (display-name [_] "note-type-view")
    om/IRender
    (render [_]
      (dom/select #js {:onChange
                       #(put! (:action-chan (om/get-shared owner))
                              [:set-recorded-sound-note-type (:id recorded-sound) (.. % -target -value)])}
        (dom/option #js {:value "eighth"} "Eighth")
        (dom/option #js {:value "quarter"} "Quarter")
        (dom/option #js {:value "half"} "Half")
        (dom/option #js {:value "whole"} "Whole")))))

;; TODO
;; Add note type drop down.
;; Move selector, then press "make sound" which will create a new "play-sound"
;; Add "Make Track" then drag play-sounds to a track.
;; Then rearrange tracks.
;; Then play button for all tracks. - use metronome code.
;; Add play head to view playback.
;; Then add saving state to index db.
;;

(defn audio-buffer-view [{:keys [recorded-sound] :as data} owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffer-view")
    om/IInitState
    (init-state [_] {:canvas nil
                     :canvas-context nil
                     :canvas-width 400
                     :canvas-height 100
                     :current-note-type :quarter})
    om/IDidMount
    (did-mount [_]
      (let [{:keys [canvas-width canvas-height]} (om/get-state owner)
            sound-name (:name recorded-sound)
            audio-buffer (:audio-buffer recorded-sound)
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")]
        (draw-buffer! canvas-width canvas-height canvas-context audio-buffer)))

    om/IRenderState
    (render-state [_ {:keys [canvas-width canvas-height current-note-type]}]
      (let [audio-buffer (:audio-buffer recorded-sound)
            sound-name (:name recorded-sound)]
      (dom/div #js {:style #js {:position "relative"}}

        (dom/div nil
           (dom/h3 #js {:style #js {:display "inline-block" :marginRight "1em"}} sound-name)
           (om/build note-type-view recorded-sound))

        (dom/canvas #js {:width  canvas-width
                         :height canvas-height
                         :ref    "canvas-ref"}
                    "no canvas")

        (om/build wave-selector-view data {:state {:recorded-sound recorded-sound :max-width canvas-width}})
        (om/build play-audio-buffer-view {:buffer-data audio-buffer :audio-context (:audio-context data)}))))))
