(ns web-audio-cljs.components.audio-buffer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put!]]
            [web-audio-cljs.components.wave-selector :refer [wave-selector-view]]
            [web-audio-cljs.state :refer [audio-context]]
            [web-audio-cljs.utils :refer [l min-arr-val max-arr-val
                                          lin-interp note-type->width
                                          recording-duration-sec]]))

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

(defn play-buffer [buffer-data offset duration]
  (let [source (.createBufferSource audio-context)
        buffer (.createBuffer audio-context 1
                              (.-length buffer-data)
                              (.-sampleRate audio-context))
        chan-data (.getChannelData buffer 0)]
    (.set chan-data buffer-data)
    (aset source "buffer" buffer)
    (.connect source (.-destination audio-context))
    (.start source 0 offset duration)))

(defn play-audio-buffer-view
  [{:keys [buffer-data play-offset play-duration]} owner]
  (reify
    om/IDisplayName (display-name [_] "play-audio-buffer-view")
    om/IRender
    (render [_]
      (dom/button #js {:onClick #(play-buffer buffer-data play-offset play-duration)}
                  "Play"))))

(defn make-button [disp-name on-click btn-label]
  (fn [data owner]
    (reify
      om/IDisplayName (display-name [_] disp-name)
      om/IRender
      (render [_] (dom/button #js {:onClick on-click} btn-label)))))

(defn note-type-view [recorded-sound owner]
  (reify
    om/IDisplayName (display-name [_] "note-type-view")
    om/IRender
    (render [_]
      (dom/select #js {:onChange
                       #(put! (:action-chan (om/get-shared owner))
                              [:set-recorded-sound-note-type recorded-sound (.. % -target -value)])
                       :value (name (:current-note-type recorded-sound))}
        (dom/option #js {:value "eighth"} "Eighth")
        (dom/option #js {:value "quarter"} "Quarter")
        (dom/option #js {:value "half"} "Half")
        (dom/option #js {:value "whole"} "Whole")))))

(defn audio-buffer-view [{:keys [bpm recorded-sound]} owner]
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
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")]
        (draw-buffer! canvas-width canvas-height canvas-context (:audio-buffer recorded-sound))))

    om/IRenderState
    (render-state [_ {:keys [canvas-width canvas-height current-note-type]}]
      (let [audio-buffer (:audio-buffer recorded-sound)
            selector-width (note-type->width (:current-note-type recorded-sound) canvas-width)
            selector-offset (:current-offset recorded-sound)
            recording-length (recording-duration-sec bpm)
            play-offset ((lin-interp 0 canvas-width 0 recording-length) selector-offset)
            play-duration (* (/ selector-width canvas-width) recording-length)
            new-play-sound-button (make-button "new-play-sound-button"
              #(put! (:action-chan (om/get-shared owner))
                      [:new-play-sound recorded-sound]) "Make Sound")]
            ;play-button (make-button "play-button-view" #(.log js/console "pushed") "Play")]
      (dom/div #js {:style #js {:position "relative"}}

        (dom/div nil
          (dom/h3 #js {:style #js {:display "inline-block" :marginRight "1em"}} (:name recorded-sound))
          (om/build note-type-view recorded-sound))

        (dom/canvas #js {:width  canvas-width
                         :height canvas-height
                         :ref    "canvas-ref"}
                    "no canvas")

        (om/build wave-selector-view recorded-sound
          {:state {:x-offset selector-offset
                   :canvas-width selector-width
                   :max-width canvas-width}})

        (om/build new-play-sound-button nil)
        (om/build play-audio-buffer-view {:buffer-data audio-buffer
                                          :play-offset play-offset
                                          :play-duration play-duration}))))))
