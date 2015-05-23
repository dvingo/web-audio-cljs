(ns web-audio-cljs.macros
  (:require [clojure.string :as string]))

(defmacro send!! [owner action & data]
  `(cljs.core.async/put! (:action-chan (om.core/get-shared ~owner))
     [~action ~@data]))

(defmacro send! [owner action & data]
  `(cljs.core.async/>! (:action-chan (om.core/get-shared ~owner))
     [~action ~@data]))

(defmacro build-button [disp-name on-click label]
  `(om.core/build
     (fn [_# owner#]
       (reify
         om.core/IDisplayName
         (~'display-name [_#] ~disp-name)
         om.core/IRender
         (~'render [_#]
           (let [classNames# (string/join " " ["button"
                                          "button--wayra"
                                          "button--border-medium"
                                          "button--text-upper"
                                          "button--size-s"
                                          "button--text-thick"
                                          "button--inverted"])]
           (om.dom/button (cljs.core/js-obj "onClick" ~on-click
             "className" classNames#) ~label)))))
     nil))
