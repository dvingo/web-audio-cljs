(ns web-audio-cljs.components.audio-buffer-list
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.components.audio-buffer :refer [audio-buffer-view]]))

(defn buffers-list-view [data owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffers-list-view")
    om/IRender
    (render [_]
      (.log js/console "num recorded sounds: " (.-length (:recorded-sounds data)))
      (apply dom/div {:className "buffers-list"}
             (map #(om/build audio-buffer-view data {:init-state {:recorded-sound %}})
                  (:recorded-sounds data))))))
