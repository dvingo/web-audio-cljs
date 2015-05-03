(ns web-audio-cljs.state
  (:require [om.core :as om :include-macros true]))

(defn save-recording! [app-state audio-recorder audio-context analyser-node]
  (let [buffer-length (.-frequencyBinCount analyser-node)]
    (.stop audio-recorder)
    (.getBuffers audio-recorder
                 (fn [buffers]
                   (om/transact! app-state :recorded-buffers #(conj % (aget buffers 0)))
                   (.clear audio-recorder)))))
