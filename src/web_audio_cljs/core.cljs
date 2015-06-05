(ns ^:figwheel-always web-audio-cljs.core
  (:require [cljs.core.async :refer [chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [app-state audio-context]]
            [web-audio-cljs.utils :refer [set-prop-if-undefined!]]
            [web-audio-cljs.components.main :refer [main-view]]))

(enable-console-print!)

(set-prop-if-undefined! "AudioContext" js/window ["AudioContext" "webkitAudioContext" "mozAudioContext"])
(set-prop-if-undefined! "getUserMedia" js/navigator ["webkitGetUserMedia" "mozGetUserMedia"])

(defonce got-audio? (atom false))

(defn got-stream [stream]
  (reset! got-audio? true)
  (let [audio-input (.createMediaStreamSource audio-context stream)
        analyser-node (.createAnalyser audio-context)]
      (set! (.-fftSize analyser-node) 2048)
      (.connect audio-input analyser-node)
      (swap! app-state assoc :audio-recorder (js/Recorder. audio-input)
                             :analyser-node analyser-node)
    (om/root main-view app-state
             {:shared {:action-chan (chan)}
              :target (. js/document (getElementById "app"))})))

(when-not @got-audio?
  (let [audio-constraints (clj->js { "audio" { "mandatory" { "googEchoCancellation" "false"
                                                             "googAutoGainControl"  "false"
                                                             "googNoiseSuppression" "false"
                                                             "googHighpassFilter"   "false" }
                                               "optional" [] }})]
    (.getUserMedia js/navigator audio-constraints
                   got-stream
                   #(.log js/console "ERROR getting user media"))))
