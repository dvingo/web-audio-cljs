(ns web-audio-cljs.components.audio-buffer-list
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.components.audio-buffer :refer [audio-buffer-view]]
            [web-audio-cljs.state :refer [recorded-sounds]]))

(defn buffers-list-view [{:keys [bpm]} owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffers-list-view")
    om/IRender
    (render [_]
      (let [rec-sounds (om/observe owner (recorded-sounds))]
        (apply dom/div {:className "buffers-list"}
          (map #(om/build audio-buffer-view {:recorded-sound % :bpm bpm}) rec-sounds))))))
