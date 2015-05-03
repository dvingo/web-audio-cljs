(ns ^:figwheel-always web-audio-cljs.core
  (:require [cljs.core.async :refer [put! chan timeout sliding-buffer <! >!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [goog.dom]
            [goog.events :as events]
            [goog.math]
            [web-audio-cljs.audio :as audio]
            [web-audio-cljs.components.audio-buffer-list :refer [buffers-list-view]]
            [web-audio-cljs.components.recorder :refer [recorder-view]]))

(enable-console-print!)

(defonce audio-context (js/window.AudioContext.))

(defonce app-state (atom {:text "Hello world!"
                          :analyser-node nil
                          :audio-recorder nil
                          :is-recording false
                          :recorded-buffers []
                          :audio-context audio-context}))

(defn play-sound [context sound-data]
  (let [audio-tag (.getElementById js/document "play-sound")]
    ;source (.createMediaElementSource context audio-tag)]
    (aset audio-tag "src" (.createObjectURL js/window.URL sound-data))
    ;(.connect source (.-destination context))
    ))

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

(defonce got-audio? (atom false))

(defn got-stream [stream]
  (reset! got-audio? true)
  (let [audio-context (:audio-context @app-state)
        input-point (.createGain audio-context)
        audio-input (.createMediaStreamSource audio-context stream)]
    (.connect audio-input input-point)
    (let [analyser-node (.createAnalyser audio-context)]
      (set! (.-fftSize analyser-node) 2048)
      (.connect input-point analyser-node)
      (swap! app-state assoc :audio-recorder (js/Recorder. input-point))
      (swap! app-state assoc :analyser-node analyser-node)
      (mount-om-root))))

(when-not @got-audio?
  (let [audio-constraints (clj->js { "audio" { "mandatory" { "googEchoCancellation" "false"
                                                             "googAutoGainControl"  "false"
                                                             "googNoiseSuppression" "false"
                                                             "googHighpassFilter"   "false" }
                                               "optional" [] }})]
    (.getUserMedia js/navigator audio-constraints
                   got-stream
                   #(.log js/console "ERROR getting user media"))))
