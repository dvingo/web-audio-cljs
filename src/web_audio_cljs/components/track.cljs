(ns web-audio-cljs.components.track
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.utils :refer [lin-interp]]
            [web-audio-cljs.state :refer [sample-from-id]]
            [web-audio-cljs.components.track-sample :refer [track-sample-view]])
  (:require-macros [web-audio-cljs.macros :refer [send!!]]))

(defn track-view [track owner]
  (reify
    om/IDisplayName (display-name [_] "track-view")
    om/IRender
    (render [_]
      (dom/div #js {:className "track"}
        (dom/input #js {:value (:name track)
                        :onChange #(send!! owner :set-track-name
                                          track (.. % -target -value))})
        (dom/span nil (:name track))
        (apply dom/div #js {:className "container"}
          (map #(let [sample (sample-from-id (:sample %))]
                  (om/build track-sample-view sample))
                  (:track-samples track)))))))
