(ns web-audio-cljs.macros)

(defmacro send! [owner action & data]
  `(cljs.core.async/put! (:action-chan (om.core/get-shared ~owner))
     [~action ~@data]))
