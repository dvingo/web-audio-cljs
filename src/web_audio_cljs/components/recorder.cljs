(ns web-audio-cljs.components.recorder
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put!]]
            [web-audio-cljs.state :refer [save-recording!]]))

(defn recorder-view [{:keys [audio-recorder is-recording audio-context analyser-node] :as data} owner]
  (reify
    om/IDisplayName (display-name [_] "recorder-view")
    om/IInitState (init-state [_] {:sound-name "Name..."})
    om/IRenderState
    (render-state [_ {:keys [sound-name]}]
      (dom/div nil
        (dom/button #js {:className "toggle-recording"
                         :onClick #(put! (:action-chan (om/get-shared owner))
                                       [:toggle-recording sound-name])}
          (if is-recording "Stop" "Record"))
        (dom/input #js {:onChange #(om/set-state! owner :sound-name (.. % -target -value))
                        :value sound-name} nil)
               ))))
