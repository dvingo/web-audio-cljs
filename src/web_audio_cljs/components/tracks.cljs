(ns web-audio-cljs.components.tracks
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [tracks]]
            [web-audio-cljs.utils :refer [make-button]]
            [web-audio-cljs.components.track :refer [track-view]])
  (:require-macros [web-audio-cljs.macros :refer [send!! build-button]]))

(defn tracks-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "tracks-view")
    om/IRender
    (render [_]
      (let [trcks (om/observe owner (tracks))]
        (dom/div nil
          (dom/div #js {:style #js {:height 80}}
            (build-button "new-track-button" #(send!! owner :make-new-track) "New Track"))
          (apply dom/div #js {:className "tracks"}
            (map #(om/build track-view %) trcks)))))))
