(ns web-audio-cljs.components.audio-buffer-list
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [web-audio-cljs.components.audio-buffer :refer [audio-buffer-view]]
            [web-audio-cljs.state :refer [sounds ui]]
            [web-audio-cljs.utils :refer [classes]])
  (:require-macros [web-audio-cljs.macros :refer [send!! p]]))

(defn buffers-list-view [_ owner]
  (reify

    om/IDisplayName (display-name [_] "audio-buffers-list-view")

    om/IRender
    (render [_]

      (let [snds (om/observe owner (sounds))
            ui (om/observe owner (ui))
            visible (:buffers-visible ui)]

        (when (> (count snds) 0)
          (dom/div #js {:className "buffers-list-wrapper"}
            (dom/div #js {:className "scroll-bar"} nil)
            (dom/h2 #js {:className "heading" :onClick #(send!! owner :toggle-buffers)}
              (dom/div #js {:className (classes (if visible "down-arrow" "up-arrow") "arrow-position")})
              "Recordings")

            (apply dom/div #js {:className "buffers"
                                :style #js {:display (if visible "block" "none")}}
              (map #(om/build audio-buffer-view %) snds))))))))
