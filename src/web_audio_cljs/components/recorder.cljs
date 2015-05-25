(ns web-audio-cljs.components.recorder
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [>! <! timeout]]
            [web-audio-cljs.state :refer [recording-duration bpm]]
            [web-audio-cljs.utils :refer [get-time-domain-data
                                          max-of-array min-of-array
                                          clear-canvas!]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [web-audio-cljs.macros :refer [send!]]))

(defn draw-circle! [canvas-context canvas-width canvas-height freq-byte-data]
  (let [max-val (max-of-array freq-byte-data)
        r (* (/ canvas-width 2) (/ max-val 256))
        center-x (/ canvas-width 2)
        center-y center-x]
    (clear-canvas! canvas-context canvas-width canvas-height nil)
    (.beginPath canvas-context)
    (.arc canvas-context center-x center-y r 0 (* 2 (.-PI js/Math)) false)
    (aset canvas-context "fillStyle" "red")
    (.fill canvas-context)))

(defn recorder-view [{:keys [audio-recorder is-recording audio-context analyser-node] :as data} owner]
  (reify
    om/IDisplayName (display-name [_] "recorder-view")
    om/IInitState (init-state [_]
      {:sound-name "Name..."
       :start-time 0
       :time-left 0
       :circle-canvas nil
       :circle-canvas-context nil
       :freq-byte-data nil})

    om/IDidMount
    (did-mount [_]
      (let [circle-canvas (om/get-node owner "circle-canvas-ref")
            freq-byte-data (get-time-domain-data analyser-node)]
        (om/update-state! owner #(assoc %
                                        :circle-canvas circle-canvas
                                        :circle-canvas-context (.getContext circle-canvas "2d")
                                        :freq-byte-data freq-byte-data))))

    om/IDidUpdate
    (did-update [_ _ {:keys [start-time]}]
      (let [time-diff (- (.now js/Date) start-time)
            {:keys [circle-canvas circle-canvas-context]} (om/get-state owner)
            freq-byte-data (get-time-domain-data analyser-node)]
        (.getByteFrequencyData analyser-node freq-byte-data)
        (when circle-canvas-context
          (draw-circle! circle-canvas-context (.-width circle-canvas) (.-height circle-canvas) freq-byte-data)
        (om/update-state! owner #(assoc % :time-left (- (recording-duration) time-diff)
                                          :freq-byte-data freq-byte-data)))))

    om/IRenderState
    (render-state [_ {:keys [sound-name time-left]}]

      (dom/div #js {:className "recording"}


        (dom/canvas #js {:width  100
                         :height 100
                         :style #js {:display (if (pos? time-left) "block" "none")}
                         :ref    "circle-canvas-ref"} "no canvas")

        (dom/img #js {:src "images/mic.svg" :width 40 :height 40
                      :alt "Record"
                      :onClick #(when-not is-recording
                                     (om/set-state! owner :start-time (.now js/Date))
                                     (go (send! owner :toggle-recording sound-name)
                                         (<! (timeout (recording-duration)))
                                         (send! owner :toggle-recording sound-name)))}
          nil)

        (when (pos? time-left) (dom/span nil (/ time-left 1000)))))))
