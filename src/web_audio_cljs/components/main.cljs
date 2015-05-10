(ns web-audio-cljs.components.main
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [start-actions-handler]]
            [web-audio-cljs.components.audio-buffer-list :refer [buffers-list-view]]
            [web-audio-cljs.components.samples :refer [samples-view]]
            [web-audio-cljs.components.chart :refer [chart-view]]
            [web-audio-cljs.components.recorder :refer [recorder-view]]))

(defn main-view [data owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (start-actions-handler (:action-chan (om/get-shared owner)) data))
    om/IRender
    (render [_]
      (dom/div nil
               (om/build recorder-view data)
               (om/build buffers-list-view data)
               (om/build samples-view data)
               (om/build chart-view data)))))
