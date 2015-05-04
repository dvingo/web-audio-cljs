(ns web-audio-cljs.components.audio-buffer
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.components.wave-selector :refer [wave-selector-view]]
            [web-audio-cljs.utils :refer [l min-arr-val max-arr-val lin-interp]]))

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

(defn play-audio-buffer-view [{:keys [buffer-data audio-context]} owner]
  (reify
    om/IDisplayName (display-name [_] "play-audio-buffer-view")
    om/IRender
    (render [_]
      (dom/button #js {
                      :onClick (fn [e]
                                 (let [source (.createBufferSource audio-context)
                                       buffer (.createBuffer audio-context 1
                                                             (.-length buffer-data) (.-sampleRate audio-context))
                                       chan-data (.getChannelData buffer 0)]
                                   (.set chan-data buffer-data)
                                   (aset source "buffer" buffer)
                                   (.connect source (.-destination audio-context))
                                   (.start source 0)))
                       }
                  "Play"))))

(defn audio-buffer-view [data owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffer-view")
    om/IInitState
    (init-state [_] {:canvas nil
                     :canvas-context nil
                     :canvas-width 400
                     :canvas-height 100})
    om/IDidMount
    (did-mount [_]
      (let [{:keys [canvas-width canvas-height recorded-sound]} (om/get-state owner)
            sound-name (:name recorded-sound)
            audio-buffer (:audio-buffer recorded-sound)
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")]
        (draw-buffer! canvas-width canvas-height canvas-context audio-buffer)))
    om/IRenderState
    (render-state [_ {:keys [canvas-width canvas-height recorded-sound]}]
      (let [audio-buffer (:audio-buffer recorded-sound)
            sound-name (:name recorded-sound)]
      (dom/div #js {:style #js {:position "relative"}}
        (dom/h3 nil sound-name)
        (dom/canvas #js {:width  canvas-width
                         :height canvas-height
                         :ref    "canvas-ref"}
                    "no canvas")
        (om/build wave-selector-view data)
        (om/build play-audio-buffer-view {:buffer-data audio-buffer :audio-context (:audio-context data)}))))))
