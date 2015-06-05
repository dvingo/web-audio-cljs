(ns web-audio-cljs.macros
  (:require [clojure.string :as string]))

(defmacro send!! [owner action & data]
  `(cljs.core.async/put! (:action-chan (om.core/get-shared ~owner))
     [~action ~@data]))

(defmacro send! [owner action & data]
  `(cljs.core.async/>! (:action-chan (om.core/get-shared ~owner))
     [~action ~@data]))

(defmacro build-button
  ([disp-name on-click label]
  `(om.core/build
     (fn [_# owner#]
       (reify
         om.core/IDisplayName
         (~'display-name [_#] ~disp-name)
         om.core/IRender
         (~'render [_#]
           (let [classNames# (string/join " " ["button"
                                          "button--nina"
                                          ;button--round-l
                                          ;button--text-thick button--inverted
                                          ;"button--border-medium"
                                          ;"button--text-upper"
                                          ;"button--size-s"
                                          "button--text-medium"
                                          ;"button--inverted"
                                          "button--round-s"])]
           (apply om.dom/button (cljs.core/js-obj "onClick" ~on-click
             "className" classNames# "data-text" ~label)
              (map #(om.dom/span nil %) ~label))))))
     nil))

  ([disp-name on-click label class-name]
  `(om.core/build
     (fn [_# owner#]
       (reify
         om.core/IDisplayName
         (~'display-name [_#] ~disp-name)

         om.core/IInitState
         (~'init-state [_#] {:mouse-down false})

         om.core/IRenderState
         (~'render-state [_# _#]
           (let [classNames# (string/join " " ["button"
                                          "button--nina"
                                          ;button--round-l
                                          ;button--text-thick button--inverted
                                          ;"button--border-medium"
                                          ;"button--text-upper"
                                          ;"button--size-s"
                                          "button--text-medium"
                                          ;"button--inverted"
                                          "button--round-s"
                                          ~class-name])]
           (apply om.dom/button
             (cljs.core/js-obj "onClick" ~on-click
                               "onMouseDown" #(om.core/update-state! owner# :mouse-down not)
                               "onMouseUp" #(om.core/update-state! owner# :mouse-down not)
                               "className" classNames#
                               "data-text" ~label)
              (map #(om.dom/span nil %) ~label))))))
     nil)))

(defmacro build-img-button [disp-name on-click img-href h w]
  `(om.core/build
     (fn [_# owner#]
       (reify
         om.core/IDisplayName (~'display-name [_#] ~disp-name)

         om.core/IInitState
         (~'init-state [_#] {:mouse-down false})

         om.core/IRenderState
         (~'render-state [_# state#]
           (om.dom/div (cljs.core/js-obj "className" "img-wrapper")
             (om.dom/img
               (cljs.core/js-obj "className" (string/join " " ["img-button" (if (:mouse-down state#) "pressed")])
                                 "onClick" ~on-click
                                 "onMouseDown" #(om.core/set-state! owner# :mouse-down true)
                                 "onMouseUp" #(om.core/set-state! owner# :mouse-down false)
                                 "onMouseLeave" #(om.core/set-state! owner# :mouse-down false)
                                 "src"     ~img-href
                                 "height"  ~h
                                 "width"   ~w))))))
     nil))

;; TODO print the file and line number that did the log.
;; Better than that - link to it on GitHub
(defmacro p [& args]
  `(.log js/console
         (-> (js/Date.) .toDateString)
         ": "
          ~@args))
