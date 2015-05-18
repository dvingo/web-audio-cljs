(ns web-audio-cljs.components.recorder
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [>! <! timeout]]
            [web-audio-cljs.utils :refer [recording-duration]]
            [web-audio-cljs.state :refer [bpm]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [web-audio-cljs.macros :refer [send!]]))

(defn recorder-view [{:keys [audio-recorder is-recording audio-context analyser-node] :as data} owner]
  (reify
    om/IDisplayName (display-name [_] "recorder-view")
    om/IInitState (init-state [_] {:sound-name "Name..." :start-time 0 :time-left 0})
    om/IDidUpdate
    (did-update [_ _ {:keys [start-time]}]
      (let [time-diff (- (.now js/Date) start-time)]
        (om/set-state! owner :time-left (- (recording-duration bpm) time-diff))))
    om/IRenderState
    (render-state [_ {:keys [sound-name time-left]}]
      (dom/div nil
        (when (pos? time-left) (dom/p nil (/ time-left 1000)))
        (dom/button #js {:className "toggle-recording"
                         :style #js {:display (if is-recording "none" "inline")}
                         :onClick #(when-not is-recording
                                     (om/set-state! owner :start-time (.now js/Date))
                                     (go (send! owner :toggle-recording sound-name)
                                         (<! (timeout (recording-duration bpm)))
                                         (send! owner :toggle-recording sound-name)))}
          "Record")
        (dom/input #js {:onChange #(om/set-state! owner :sound-name (.. % -target -value))
                        :value sound-name} nil)))))
