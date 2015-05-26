(ns web-audio-cljs.components.samples
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [samples]]
            [web-audio-cljs.components.sample :refer [sample-view]]))

(defn samples-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "samples-view")
    om/IRender
    (render [_]
      (let [smples (om/observe owner (samples))]
        (dom/div nil
          (dom/h2 #js {:className "heading"} "Samples")
          (apply dom/div #js {:className "samples"}
                 (map #(om/build sample-view %) smples)))))))
