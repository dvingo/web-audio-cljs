(ns web-audio-cljs.components.audio-buffer-list
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.components.audio-buffer :refer [audio-buffer-view]]
            [web-audio-cljs.state :refer [sounds ui]])
  (:require-macros [web-audio-cljs.macros :refer [build-button send!!]]))

(defn buffers-list-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffers-list-view")
    om/IRender
    (render [_]
      (let [snds (om/observe owner (sounds))
            ui (om/observe owner (ui))
            visible (:buffers-visible ui)]
        (dom/div #js {:style #js {:float "left"}}
          (build-button "toggle-buffers-view"
            #(send!! owner :toggle-buffers) "Toggle Buffers")

          (apply dom/div #js {:style #js
            {:float "left" :display (if visible "block" "none")}}
            (map #(om/build audio-buffer-view %) snds)))))))
