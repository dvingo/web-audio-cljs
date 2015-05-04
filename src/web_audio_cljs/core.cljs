(ns ^:figwheel-always web-audio-cljs.core
  (:require [cljs.core.async :refer [chan]]
            [cljs-uuid.core :as uuid]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.audio :as audio]
            [web-audio-cljs.state :refer [actions-handler]]
            [web-audio-cljs.components.audio-buffer-list :refer [buffers-list-view]]
            [web-audio-cljs.components.recorder :refer [recorder-view]]))

(enable-console-print!)

(defonce audio-context (js/window.AudioContext.))

#_{:compositions [{:id (uuid/make-random) :name "First composition" :tracks [track-id1 track-id2]}]
          :tracks [{:id track-id1 :name "First track" :play-sounds [play-sound-id1 play-sound-id2 play-sound-id3]}
                   {:id track-id2 :name "Second track" :play-sounds [play-sound-id4 play-sound-id5 play-sound-id6]}]
          :recorded-sounds [{:id recorded-sound-id1 :name "beep" :audio-buffer nil}
                            {:id recorded-sound-id2 :name "bleep" :audio-buffer nil}
                            {:id recorded-sound-id3 :name "bloop" :audio-buffer nil}
                            {:id recorded-sound-id4 :name "bop" :audio-buffer nil}]
          :play-sounds [{:id play-sound-id1 :recorded-sound recorded-sound-id1 :type :quarter :offset 7998}
                        {:id play-sound-id2 :recorded-sound recorded-sound-id2 :type :whole :offset 4498}
                        {:id play-sound-id3 :recorded-sound recorded-sound-id3 :type :quarter :offset 8998}
                        {:id play-sound-id4 :recorded-sound recorded-sound-id4 :type :eigth :offset 998}]}

(let [db {:compositions []
          :tracks []
          :recorded-sounds []
          :play-sounds []
          :analyser-node nil
          :audio-recorder nil
          :is-recording false
          :audio-context audio-context}]
  (defonce app-state (atom db)))

(defn play-sound [context sound-data]
  (let [audio-tag (.getElementById js/document "play-sound")]
    (aset audio-tag "src" (.createObjectURL js/window.URL sound-data))))

(defn mount-om-root []
  (let [action-chan (chan)]
    (om/root
      (fn [data owner]
        (reify
          om/IWillMount
          (will-mount [_] (actions-handler action-chan data))
          om/IRender
          (render [_]
            (dom/div nil
                     (om/build recorder-view data)
                     (om/build buffers-list-view data)
                     (om/build audio/chart-view data)))))
      app-state
      {:shared {:action-chan action-chan}
       :target (. js/document (getElementById "app"))})))

(defonce got-audio? (atom false))

(defn got-stream [stream]
  (reset! got-audio? true)
  (let [audio-context (:audio-context @app-state)
        audio-input (.createMediaStreamSource audio-context stream)]
    (let [analyser-node (.createAnalyser audio-context)]
      (set! (.-fftSize analyser-node) 2048)
      (.connect audio-input analyser-node)
      (swap! app-state assoc :audio-recorder (js/Recorder. audio-input)
                             :analyser-node analyser-node)
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
