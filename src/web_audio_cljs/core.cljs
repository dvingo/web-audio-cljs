(ns ^:figwheel-always web-audio-cljs.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [web-audio-cljs.audio :as audio]
              [web-audio-cljs.utils :refer [l]]))

(enable-console-print!)
(defonce audio-context (js/window.AudioContext.))
(defonce app-state (atom {:text "Hello world!"
                          :analyser-node nil
                          :audio-recorder nil
                          :is-recording false
                          :audio-context audio-context}))

(defn draw-buffer! [width height canvas-context data]
  (let [step (.ceil js/Math (/ (.-length data) width))
        amp (/ height 2)]
    (aset canvas-context "fillStyle" "silver")
    (.clearRect canvas-context 0 0 width height)
    (.log js/console "amplitude" amp)
    (.log js/console "data length " (.-length data))
    (.log js/console "step " step)
    (.log js/console "width " width)
    (doseq [i (range width)]
      (doseq [j (range step)]
        (let [datum (aget data (+ (* i step) j))]
          (.fillRect canvas-context i amp 1 (- (.max js/Math 1 (* datum amp)))))))))

(defn play-sound [context sound-data]
  (let [audio-tag (.getElementById js/document "play-sound")]
    ;source (.createMediaElementSource context audio-tag)]
    (aset audio-tag "src" (.createObjectURL js/window.URL sound-data))
    ;(.connect source (.-destination context))
    ))

(defn save-recording [recorder output-canvas-id audio-context]
 (.stop recorder)
 (.log js/console "HERE IN EXPROT")
 (.getBuffers recorder
              (fn [buffers]
                (.log js/console "GOT BUFFERS" buffers)
                (let [canvas (.getElementById js/document output-canvas-id)
                      canvas-context (.getContext canvas "2d")
                      canvas-width (.-width canvas)
                      canvas-height (.-height canvas)]
                  (draw-buffer! canvas-width canvas-height canvas-context (aget buffers 0)))
                (.exportWAV recorder (fn [blob]
                                       (let [buffer (aget buffers 0)]
                                         (play-sound audio-context blob)
                                         (.log js/console "GOT RECORDING: " blob)
                                         (.clear recorder)
                                         (.setupDownload js/Recorder blob "templ.wav")))))))

(defn audio-view [{:keys [audio-recorder is-recording audio-context] :as data} owner]
  (reify
    om/IDisplayName (display-name [_] "audio-view")
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h1 nil "HEllo")
        (dom/div #js {:className "record"
                         :onClick (fn [e]
                                    (.log js/console "CLICK")
                                    (.log js/console "audio recorder: " audio-recorder)
                                    (.log js/console "is-recording: " is-recording)
                                    (if is-recording
                                      (do
                                        (save-recording audio-recorder "buffer-display" audio-context)
                                        (om/transact! data :is-recording not))
                                      (do
                                        (.record audio-recorder)
                                        (om/transact! data :is-recording not))))}
                    (if is-recording "Stop" "Record"))))))

(defn mount-om-root []
  (om/root
    (fn [data owner]
      (reify
        om/IRender
        (render [_]
          (dom/div nil
                   (om/build audio-view data)
                   (om/build audio/chart-view data)))))
    app-state
    {:target (. js/document (getElementById "app"))}))

(defn got-stream [stream]
  (let [audio-context (:audio-context @app-state)
        input-point (.createGain audio-context)
        audio-input (.createMediaStreamSource audio-context stream)]
    (.connect audio-input input-point)
    (let [analyser-node (.createAnalyser audio-context)]
      (set! (.-fftSize analyser-node) 2048)
      (.connect input-point analyser-node)
      (let [zero-gain (.createGain audio-context)]
        (set! (-> zero-gain .-gain .-value) 0.0)
        (.connect input-point zero-gain)
        (.connect zero-gain  (.-destination audio-context))
        (swap! app-state assoc :audio-recorder (js/Recorder. input-point))
        (swap! app-state assoc :analyser-node analyser-node)
        (mount-om-root)))))

(let [audio-constraints (clj->js { "audio" { "mandatory" { "googEchoCancellation" "false"
                                                           "googAutoGainControl"  "false"
                                                           "googNoiseSuppression" "false"
                                                           "googHighpassFilter"   "false" }
                                             "optional" [] }})]
  (.getUserMedia js/navigator audio-constraints
                 got-stream
                 #(.log js/console "ERROR getting user media")))
