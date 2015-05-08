(ns web-audio-cljs.components.audio-buffer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put!]]
            [web-audio-cljs.components.wave-selector :refer [wave-selector-view]]
            [web-audio-cljs.state :refer [audio-context wave-width wave-height
                                          note-type->width note-types]]
            [web-audio-cljs.utils :refer [l min-arr-val max-arr-val
                                          lin-interp
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

(defn note-type-view [sound owner]
  (reify
    om/IDisplayName (display-name [_] "note-type-view")
    om/IRender
    (render [_]
      (apply dom/select #js {:onChange
                       #(put! (:action-chan (om/get-shared owner))
                              [:set-sound-note-type sound (.. % -target -value)])
                       :value (:current-note-type sound)}
        (map #(dom/option #js {:value %} %) note-types)))))

(defn audio-buffer-view [{:keys [bpm sound]} owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffer-view")
    om/IInitState
    (init-state [_] {:canvas nil
                     :canvas-context nil
                     :current-note-type :quarter})
    om/IDidMount
    (did-mount [_]
      (let [sound-name (:name sound)
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")]
        (draw-buffer! wave-width wave-height canvas-context (:audio-buffer sound))))

    om/IRenderState
    (render-state [_ {:keys [current-note-type]}]
      (.log js/console "(:current-note-type sound): " (:current-note-type sound))
      (.log js/console "(get note-type->width (:current-note-type sound)): "(get note-type->width (:current-note-type sound)))
      (let [audio-buffer (:audio-buffer sound)
            selector-width (get note-type->width (:current-note-type sound))
            selector-offset (:current-offset sound)
            recording-length (recording-duration-sec bpm)
            play-offset ((lin-interp 0 wave-width 0 recording-length) selector-offset)
            play-duration (* (/ selector-width wave-width) recording-length)
            make-sample-button (make-button "make-sample-button"
              #(put! (:action-chan (om/get-shared owner))
                      [:new-sample sound]) "Make Sample")]

      (dom/div #js {:style #js {:position "relative"}}

        (dom/div nil
          (dom/h3 #js {:style #js {:display "inline-block" :marginRight "1em"}} (:name sound))
          (om/build note-type-view sound))

        (dom/canvas #js {:width  wave-width
                         :height wave-height
                         :ref    "canvas-ref"}
                    "no canvas")

        (om/build wave-selector-view sound
          {:state {:x-offset selector-offset
                   :canvas-width selector-width
                   :max-width wave-width}})

        (om/build make-sample-button nil)
        (om/build play-audio-buffer-view {:buffer-data audio-buffer
                                          :play-offset play-offset
                                          :play-duration play-duration}))))))
