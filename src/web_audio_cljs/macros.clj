(ns web-audio-cljs.macros)

(defmacro send! [owner action & data]
  `(cljs.core.async/put! (:action-chan (om.core/get-shared ~owner))
     [~action ~@data]))


(defmacro build-button [disp-name on-click label]
  `(om.core/build
     (fn [~(symbol "_") ~(symbol "owner")]
       (reify
         om.core/IDisplayName
         (~(symbol "display-name") [~(symbol "_")] ~disp-name)
         om.core/IRender
         (~(symbol "render") [~(symbol "_")]
           (om.dom/button (cljs.core/js-obj "onClick" ~on-click) ~label))))
     nil))
