(ns web-audio-cljs.state
  (:require [om.core :as om :include-macros true]
            [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [put! chan timeout <! >! onto-chan]]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(defn new-recorded-sound [audio-buffer aname]
  {:id (uuid/make-random) :name aname :audio-buffer audio-buffer})

(defn save-recording!
  [{:keys [audio-recorder audio-context analyser-node] :as app-state} sound-name]
  (let [buffer-length (.-frequencyBinCount analyser-node)]
    (.stop audio-recorder)
    (.getBuffers audio-recorder
                 (fn [buffers]
                   (om/transact! app-state :recorded-sounds
                                 #(conj % (new-recorded-sound (aget buffers 0) sound-name)))
                   (.clear audio-recorder)))))

(defn handle-toggle-recording [{:keys [is-recording audio-recorder] :as app-state} sound-name]
  (if is-recording
    (save-recording! app-state sound-name)
    (.record audio-recorder))
  (om/transact! app-state :is-recording not))

(defn actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
     (match [action-vec]
       [[:toggle-recording sound-name]] (handle-toggle-recording app-state sound-name))
     (recur (<! actions-chan))))
