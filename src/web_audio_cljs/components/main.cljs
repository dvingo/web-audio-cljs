(ns web-audio-cljs.components.main
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [start-actions-handler]]
            [web-audio-cljs.components.audio-buffer-list :refer [buffers-list-view]]
            [web-audio-cljs.components.samples :refer [samples-view]]
            [web-audio-cljs.components.tracks :refer [tracks-view]]
            [web-audio-cljs.components.chart :refer [chart-view]]
            [web-audio-cljs.components.play :refer [play-view]]
            [web-audio-cljs.components.recorder :refer [recorder-view]]))

(defn main-view [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (start-actions-handler (:action-chan (om/get-shared owner)) data))
    om/IRender
    (render [_]
      (dom/div #js {:className "container"}

        (dom/div #js {:className "middle"}

          (dom/div #js {:className "top-bar"}
            (dom/div #js {:className "left-top"}
              (dom/img #js {:src "images/cljs.png"
                           :width 70 :height 70} nil)
              (dom/h1 nil "Music"))

            (dom/div #js {:className "vis"}
              (om/build chart-view data)
              (om/build recorder-view data)))

          (dom/div #js {:className "middle-container"}

            (dom/div #js {:className "left-bar"}
              (om/build buffers-list-view data)
              (om/build samples-view data))

            (dom/div #js {:className "tracks-container"}
              (om/build tracks-view data))))

          (om/build play-view data)))))
