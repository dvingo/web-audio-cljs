(ns ^:figwheel-always web-audio-cljs.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.dom :as googdom]
            [goog.events :as events]
            [web-audio-cljs.audio :as audio]
            [web-audio-cljs.utils :refer [l]]
            [cljs.core.async :refer [put! chan timeout sliding-buffer <! >!]])
  (:import [goog.events EventType])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(enable-console-print!)

(defonce audio-context (js/window.AudioContext.))
(defonce app-state (atom {:text "Hello world!"
                          :analyser-node nil
                          :audio-recorder nil
                          :is-recording false
                          :recorded-buffers []
                          :audio-context audio-context}))

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
      (fn [e] (put! out e)))
    out))

(defn lin-interp [x0 x1 y0 y1]
  (fn [x]
    (+ y0
       (* (/ (- x x0)
             (- x1 x0))
          (- y1 y0)))))

(defn min-arr-val [arr]
  (loop [i 0 min-val (aget arr 0)]
    (if (= i (.-length arr))
      min-val
      (let [cur-val (aget arr i)]
        (if (< cur-val min-val)
          (recur (inc i) cur-val)
          (recur (inc i) min-val))))))

(defn max-arr-val [arr]
  (loop [i 0 max-val (aget arr 0)]
    (if (= i (.-length arr))
      max-val
      (let [cur-val (aget arr i)]
        (if (> cur-val max-val)
          (recur (inc i) cur-val)
          (recur (inc i) max-val))))))

(defn draw-select-rect! [canvas-width canvas-height canvas-context x mouse-down?]
  (.clearRect canvas-context 0 0 canvas-width canvas-height)
  (.fillRect canvas-context x 0 100 100)
  (when mouse-down?
      (aset canvas-context "strokeStyle" "aliceblue")
      (aset canvas-context "lineWidth" 6)
      (.strokeRect canvas-context x 2 100 96)))


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

(defn play-sound [context sound-data]
  (let [audio-tag (.getElementById js/document "play-sound")]
    ;source (.createMediaElementSource context audio-tag)]
    (aset audio-tag "src" (.createObjectURL js/window.URL sound-data))
    ;(.connect source (.-destination context))
    ))

(defn save-recording! [app-state audio-recorder audio-context analyser-node]
  (let [buffer-length (.-frequencyBinCount analyser-node)]
    (.stop audio-recorder)
    (.getBuffers audio-recorder
                 (fn [buffers]
                   (om/transact! app-state :recorded-buffers #(conj % (aget buffers 0)))
                   (.clear audio-recorder)))))

(defn recorder-view [{:keys [audio-recorder is-recording audio-context analyser-node] :as data} owner]
  (reify
    om/IDisplayName (display-name [_] "recorder-view")
    om/IRender
    (render [_]
      (dom/div nil
               (dom/button #js {:className "toggle-recording"
                                :onClick (fn [e]
                                           (if is-recording
                                             (do
                                               (save-recording! data audio-recorder audio-context analyser-node)
                                               (om/transact! data :is-recording not))
                                             (do
                                               (.record audio-recorder)
                                               (om/transact! data :is-recording not))))}
                           (if is-recording "Stop" "Record"))))))

(defn wave-selector-view [data owner]
  (reify
    om/IDisplayName (display-name [_] "wave-selector-view")

    om/IInitState
    (init-state [_] {:canvas nil
                     :canvas-context nil
                     :canvas-width 400
                     :canvas-height 100
                     :mouse-down false
                     :x-offset 0})

    om/IDidMount
    (did-mount [_]
      (let [{:keys [canvas-width canvas-height left-x x-offset]} (om/get-state owner)
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")
            x-pos (.-left (.getBoundingClientRect canvas))
            mouse-move-chan (listen (googdom/getElement js/document) (.-MOUSEMOVE EventType))]
        (go
          (while true
            (let [e (<! mouse-move-chan)
                  x (.-clientX e)
                  rel-x (- (- x x-pos) (/ 100 2))
                  {:keys [mouse-down]} (om/get-state owner)]
              (when mouse-down
                (.log js/console "!!!clientX" x)
                (.log js/console "OFFSET x: " rel-x)
                (om/update-state! owner #(assoc % :x-offset rel-x))))))
        (om/update-state! owner #(assoc % :canvas-context canvas-context :canvas canvas))))

    om/IDidUpdate
    (did-update [_ _ _]
      (let [{:keys [canvas-width canvas-height canvas-context x-offset mouse-down]} (om/get-state owner)]
        (if canvas-context
          (draw-select-rect! canvas-width canvas-height canvas-context x-offset mouse-down)
          (om/refresh! owner))))

    om/IRenderState
    (render-state [_ {:keys [canvas-width canvas-height mouse-down]}]
      (dom/canvas #js {:width       canvas-width
                       :height      canvas-height
                       :style       #js {:opacity 0.3 :position "relative" :left (- canvas-width)}
                       :ref         "canvas-ref"
                       :onMouseDown #(om/set-state! owner :mouse-down true)
                       :onMouseUp   #(om/set-state! owner :mouse-down false)}
                  "no canvas"))))

(defn buffer-view [data owner]
  (reify
    om/IDisplayName (display-name [_] "buffer-view")

    om/IInitState
    (init-state [_] {:canvas nil
                     :canvas-context nil
                     :canvas-width 400
                     :canvas-height 100})

    om/IDidMount
    (did-mount [_]
      (let [{:keys [canvas-width canvas-height buffer]} (om/get-state owner)
            canvas (om/get-node owner "canvas-ref")
            canvas-context (.getContext canvas "2d")]
        (draw-buffer! canvas-width canvas-height canvas-context buffer)))

    om/IRenderState
    (render-state [_ {:keys [canvas-width canvas-height]}]
      (dom/div nil
        (dom/canvas #js {:width canvas-width
                         :height canvas-height
                         :ref "canvas-ref"} "no canvas")
               (om/build wave-selector-view data)))))

(defn buffers-list-view [data owner]
  (reify
    om/IDisplayName (display-name [_] "buffers-list-view")
    om/IRender
    (render [_]
      (apply dom/div {:className "buffers-list"}
             (map #(om/build buffer-view data {:init-state {:buffer %}})
                  (:recorded-buffers data))))))

(defn mount-om-root []
  (om/root
    (fn [data owner]
      (reify
        om/IRender
        (render [_]
          (dom/div nil
                   (om/build recorder-view data)
                   (om/build buffers-list-view data)
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
