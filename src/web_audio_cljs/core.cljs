(ns ^:figwheel-always web-audio-cljs.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [web-audio-cljs.audio :as audio]
              [web-audio-cljs.utils :refer [l]]))

(enable-console-print!)
(defonce AudioContext (.-AudioContext js/window))
(defonce audio-context (AudioContext.))
(defonce app-state (atom {:text "Hello world!"
                          :analyser-node nil
                          :audio-recorder nil
                          :audio-context audio-context}))

(defn audio-view [data cursor]
  (reify
    om/IDisplayName (display-name [_] "audio-view")
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h1 nil "HEllo")
        (dom/button #js {:onClick (fn [e] (.log js/console "CLICK"))}
                    "Button")))))

(defn mount-om-root []
  (om/root
    (fn [data owner]
      (reify
        om/IRender
        (render [_]
          (dom/div nil
                   (om/build audio-view data)
                   (om/build audio/chart-view data)
                   (dom/h1 nil "HEREEE")))))
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
