(ns web-audio-cljs.components.sample
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [sample-width sample-height
                                          note-type->bg-color
                                          note-type->color
                                          note-type->arc note-type->rotate-path]]
            [cljs.core.async :refer [put!]]))

(def fill-color "white")

(defn sample-view [sample owner]
  (reify
    om/IDisplayName (display-name [_] "sample-view")
    om/IRender (render [_]
                 (let [note-type (:type sample)]
                   (dom/div #js {:className "sample"
                                 :style #js
                                 {:width sample-width
                                  :height sample-height
                                  :color (get note-type->color note-type)
                                  :background (get note-type->bg-color note-type)
                                  :padding (/ sample-width 10)
                                  :borderRadius (/ sample-width 10)}}
                          (dom/p #js {:className "name"} (:name sample))
                            (dom/svg #js {:viewBox "0 0 1 1" :width "20"
                                         :style #js {:position "absolute" :top "10px" :right "10px"}}
                                     (when (= note-type "Whole")
                                       (dom/circle #js {:cx ".5" :cy ".5" :r ".5" :strokeWidth ".01px" :stroke fill-color :fill fill-color}))

                                     (when-not (= note-type "Whole")
                                         (dom/circle #js {:cx ".5" :cy ".5" :r ".5" :strokeWidth ".01px" :stroke fill-color :fill "none"}))
                                     (when-not (= note-type "Whole")
                                         (dom/path #js {:d (get note-type->arc note-type) :fill fill-color
                                                          :transform (get note-type->rotate-path note-type)})))
                          #_(dom/h5 #js {:className "note-type"}
                                  (first note-type)))))))
