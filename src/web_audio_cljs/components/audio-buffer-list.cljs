(ns web-audio-cljs.components.audio-buffer-list
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.components.audio-buffer :refer [audio-buffer-view]]))

(defn buffers-list-view [data owner]
  (reify
    om/IDisplayName (display-name [_] "audio-buffers-list-view")
    om/IRender
    (render [_]
      (apply dom/div {:className "buffers-list"}
        (map #(om/build audio-buffer-view data {:fn (fn [d] (assoc d :recorded-sound %))})
             (:recorded-sounds data))))))
