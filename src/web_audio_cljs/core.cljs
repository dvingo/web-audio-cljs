(ns ^:figwheel-always web-audio-cljs.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [web-audio-cljs.audio :as audio]
              ))

(enable-console-print!)
(defonce app-state (atom {:text "Hello world!"}))

(defn audio-view [data cursor]
  (reify
    om/IDisplayName (display-name [_] "audio-view")
    om/IRender
    (render [_]
      (dom/div nil
        (dom/h1 nil "HEllo")
        (dom/button #js { :onClick (fn [e]
                                     (.log js/console "CLICK"))
                         }
                    "Button")))))

(om/root
  (fn [data owner]
    (reify
      om/IRender
      (render [_]
        (dom/div nil
                 (om/build audio-view data)
                 (dom/h1 nil "HEREEE")))))
  app-state
  {:target (. js/document (getElementById "app"))})
