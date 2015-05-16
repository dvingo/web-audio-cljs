(ns web-audio-cljs.components.audio-buffer-list
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.components.audio-buffer :refer [audio-buffer-view]]
            [web-audio-cljs.utils :refer [make-button]]
            [web-audio-cljs.state :refer [sounds]])
  (:require-macros [web-audio-cljs.macros :refer [send!!]]))

(defn buffers-list-view [{:keys [bpm ui]} owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffers-list-view")
    om/IRender
    (render [_]
      (let [snds (om/observe owner (sounds))
            visible (:buffers-visible ui)]
        (dom/div nil
          (om/build
            (make-button "toggle-buffers-view"
                         #(send!! owner :toggle-buffers) "Toggle Buffers") nil)

             (apply dom/div #js {:className "buffers-list"
                                 :style #js {:width "50%" :float "left"
                                             :display (if visible "block" "none")}}
                    (map #(om/build audio-buffer-view {:sound % :bpm bpm}) snds)))))))
