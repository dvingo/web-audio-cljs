(ns web-audio-cljs.components.recorder
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [save-recording!]]))

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
