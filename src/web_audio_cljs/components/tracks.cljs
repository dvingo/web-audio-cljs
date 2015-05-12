(ns web-audio-cljs.components.tracks
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [tracks]]
            [web-audio-cljs.utils :refer [make-button]]
            [web-audio-cljs.components.track :refer [track-view]]
            [cljs.core.async :refer [put!]]))

(defn tracks-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "tracks-view")
    om/IRender
    (render [_]
      (let [trcks (om/observe owner (tracks))
            new-track-btn (make-button "new-track-button"
                            #(put! (:action-chan (om/get-shared owner))
                                   [:make-new-track]) "New Track")]
        (dom/div nil
          (om/build new-track-btn nil)
          (apply dom/div #js {:className "tracks"}
            (map #(om/build track-view %) trcks)))))))
