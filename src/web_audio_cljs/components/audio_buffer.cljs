(ns web-audio-cljs.components.audio-buffer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.components.wave-selector :refer [wave-selector-view]]
            [web-audio-cljs.state :refer [audio-context wave-width wave-height
                                          note-type->width note-types bpm
                                          recording-duration-sec play-buffer!]]
            [web-audio-cljs.utils :refer [l min-arr-val max-arr-val
                                          lin-interp]])
  (:require-macros [web-audio-cljs.macros :refer [send!! build-img-button build-button]]))

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

(defn note-type-view [sound owner]
  (reify
    om/IDisplayName (display-name [_] "note-type-view")
    om/IRender
    (render [_]
      (apply dom/select #js
        {:onChange #(send!! owner :set-sound-note-type sound (.. % -target -value))
         :value (:current-note-type sound)}
        (map #(dom/option #js {:value %} %) note-types)))))

(defn audio-buffer-view [sound owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffer-view")
    om/IInitState
    (init-state [_] {:canvas nil
                     :canvas-context nil
                     :current-note-type :quarter
                     :highlight-name false})
    om/IDidMount
    (did-mount [_]
      (let [sound-name (:name sound)
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")]
        (draw-buffer! wave-width wave-height canvas-context (:audio-buffer sound))))

    om/IRenderState
    (render-state [_ {:keys [current-note-type highlight-name]}]
      (let [audio-buffer (:audio-buffer sound)
            selector-width (get note-type->width (:current-note-type sound))
            selector-offset (:current-offset sound)
            recording-length (recording-duration-sec)
            play-offset ((lin-interp 0 wave-width 0 recording-length) selector-offset)
            play-duration (* (/ selector-width wave-width) recording-length)]

      (dom/div #js {:className "buffer"}

        (dom/div #js {:className "buffer-top-section"}
          (dom/div nil
            (if (:editing-name sound)
              (dom/div nil
                (dom/input #js {:className "buffer-name-input"
                               :onChange #(send!! owner :set-sound-name sound (.. % -target -value))
                               :onKeyPress #(when (= (.-key %) "Enter")
                                              (om/set-state! owner :highlight-name false)
                                              (send!! owner :toggle-sound-name-edit sound))
                                :value (:name sound)} nil))
              (dom/h3 #js {:className "buffer-name"
                           :style #js {:text-decoration (if highlight-name "underline" "none")}
                           :onMouseOver #(om/set-state! owner :highlight-name true)
                           :onMouseOut #(om/set-state! owner :highlight-name false)
                           :onDoubleClick #(send!! owner :toggle-sound-name-edit sound)} (:name sound)))
            (om/build note-type-view sound))

          (dom/div #js {:className "button-container"}
            (build-button "make-sample-button"
              #(send!! owner :new-sample sound) "Make Sample" "buffer-make-sample-button")

            (build-img-button "play-audio-buffer-view"
              #(play-buffer! audio-context audio-buffer play-offset play-duration) "images/play_button2.svg" 40 40)))

        (dom/div #js {:className "wave-container"}
          (dom/canvas #js {:width  wave-width
                           :height wave-height
                           :ref    "canvas-ref"}
                      "no canvas")

          (om/build wave-selector-view sound
            {:state {:x-offset selector-offset
                     :canvas-width selector-width
                     :max-width wave-width}})))))))
