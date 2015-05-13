(ns ^:figwheel-always web-audio-cljs.core
  (:require [cljs.core.async :refer [chan]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [start-actions-handler app-state audio-context]]
            [web-audio-cljs.utils :refer [set-prop-if-undefined!]]
            [web-audio-cljs.components.main :refer [main-view]]))

(enable-console-print!)

(set-prop-if-undefined! "AudioContext" js/window ["AudioContext" "webkitAudioContext" "mozAudioContext"])
(set-prop-if-undefined! "getUserMedia" js/navigator ["webkitGetUserMedia" "mozGetUserMedia"])
(set-prop-if-undefined! "cancelAnimationFrame" js/window
                        ["webkitCancelAnimationFrame" "mozCancelAnimationFrame"])
(set-prop-if-undefined! "requestAnimationFrame" js/window
                        ["webkitRequestAnimationFrame" "mozRequestAnimationFrame"])

#_{:compositions [{:id (uuid/make-random) :name "First composition" :tracks [track-id1 track-id2]}]
          :tracks [{:id track-id1 :name "First track" :track-samples [sample-id1 sample-id2 sample-id3]}
                   {:id track-id2 :name "Second track" :track-samples [sample-id4 sample-id5 sample-id6]}]
          :track-samples [{:id track-sample-id1 :sample sample-id1 :offset 98}
                        {:id track-sample-id2 :sample sample-id2 :offset 12}]
          :sounds [{:id sound-id1 :name "beep" :audio-buffer nil :current-note-type :quarter}
                            {:id sound-id2 :name "bleep" :audio-buffer nil :current-note-type :quarter}
                            {:id sound-id3 :name "bloop" :audio-buffer nil :current-note-type :quarter}
                            {:id sound-id4 :name "bop" :audio-buffer nil :current-note-type :quarter}]
          :samples [{:id sample-id1 :sound sound-id1 :type :quarter :offset 7998}
                        {:id sample-id2 :sound sound-id2 :type :whole :offset 4498}
                        {:id sample-id3 :sound sound-id3 :type :half :offset 8998}
                        {:id sample-id4 :sound sound-id4 :type :eighth :offset 998}]}

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
