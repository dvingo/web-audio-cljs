(ns web-audio-cljs.components.track
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [web-audio-cljs.macros :refer [send!]]))

(defn track-view [track owner]
  (reify
    om/IDisplayName (display-name [_] "track-view")
    om/IRender (render [_]
                 (dom/div #js {:className "track"}
                   (dom/input #js {:value (:name track)
                                   :onChange #(send! owner :set-track-name track (.. % -target -value))})
                     (dom/p nil (:name track))))))
