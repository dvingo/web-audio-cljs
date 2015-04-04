(ns ^:figwheel-always web-audio-cljs.core
    (:require[om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Audio Functions                                                           ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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
    (reify om/IRender
      (render [_]
        (dom/div nil
                 (om/build audio-view data)))))
  app-state
  {:target (. js/document (getElementById "app"))})
