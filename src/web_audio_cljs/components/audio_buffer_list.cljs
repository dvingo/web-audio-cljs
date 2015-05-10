(ns web-audio-cljs.components.audio-buffer-list
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.components.audio-buffer :refer [audio-buffer-view]]
            [web-audio-cljs.state :refer [sounds]]))

(defn buffers-list-view [{:keys [bpm]} owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffers-list-view")
    om/IRender
    (render [_]
      (let [snds (om/observe owner (sounds))]
        (apply dom/div #js {:className "buffers-list" :style #js {:width "50%" :float "left"}}
          (map #(om/build audio-buffer-view {:sound % :bpm bpm}) snds))))))
