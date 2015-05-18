(ns web-audio-cljs.components.track
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.utils :refer [lin-interp]]
            [web-audio-cljs.state :refer [ui track-samples-for-track track-width]]
            [web-audio-cljs.components.track-sample :refer [track-sample-view]])
  (:require-macros [web-audio-cljs.macros :refer [send!!]]))

(defn track-view [track owner]
  (reify
    om/IDisplayName (display-name [_] "track-view")
    om/IRender
    (render [_]
      (let [ui (om/observe owner (ui))
            selected? (= (:selected-track-id ui) (:id track))]

        (dom/div #js {:className "track"}

          (dom/input #js
            {:value (:name track)
             :onChange #(send!! owner :set-track-name track (.. % -target -value))})

          (dom/span nil (:name track))

          (apply dom/div #js
            {:className "container"
             :style #js {:width track-width
                         :border
                         (if selected? "3px dashed" "1px solid black")}
             :onClick #(when-not selected? (send!! owner :select-track track))}
            (map #(om/build track-sample-view %) (track-samples-for-track track))))))))
