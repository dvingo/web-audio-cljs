(ns web-audio-cljs.state
  (:require [om.core :as om :include-macros true]
            [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [put! chan timeout <! >! onto-chan]]
            [cljs.core.match :refer-macros [match]]
            [web-audio-cljs.utils :refer [get-val-with-index]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; recorded-sounds.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn new-recorded-sound [audio-buffer aname]
  {:id (uuid/make-random)
   :name aname
   :audio-buffer audio-buffer
   :current-offset 0
   :current-note-type :quarter})

(defn save-recording! [app-state sound-name]
  (let [{:keys [audio-recorder audio-context analyser-node]} @app-state
        buffer-length (.-frequencyBinCount analyser-node)]
    (.stop audio-recorder)
    (.getBuffers audio-recorder
                 (fn [buffers]
                   (om/transact! app-state :recorded-sounds
                                 #(conj % (new-recorded-sound (aget buffers 0) sound-name)))
                   (.clear audio-recorder)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-toggle-recording [app-state sound-name]
  (let [{:keys [is-recording audio-recorder]} @app-state]
    (if is-recording
      (save-recording! app-state sound-name)
      (.record audio-recorder))
    (om/transact! app-state :is-recording not)))

(defn handle-update-recorded-sound-note-type [app-state recorded-sound-id note-type]
  (let [[recorded-sound i] (get-val-with-index :id recorded-sound-id (:recorded-sounds @app-state))
        new-recorded-sound (assoc recorded-sound :current-note-type note-type)]
    (om/transact! app-state :recorded-sounds #(assoc % i new-recorded-sound))))

(defn handle-update-recorded-sound-offset [app-state recorded-sound-id x-offset]
  (let [[recorded-sound i] (get-val-with-index :id recorded-sound-id (:recorded-sounds @app-state))
        new-recorded-sound (assoc recorded-sound :current-offset x-offset)]
    (om/transact! app-state :recorded-sounds #(assoc % i new-recorded-sound))))

(defn actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
     (match [action-vec]
       [[:toggle-recording sound-name]] (handle-toggle-recording app-state sound-name)
       [[:set-recorded-sound-note-type recorded-sound-id note-type]]
            (handle-update-recorded-sound-note-type app-state recorded-sound-id note-type)
       [[:set-recorded-sound-offset recorded-sound-id x-offset]]
            (handle-update-recorded-sound-offset app-state recorded-sound-id x-offset)
       :else (.log js/console "Unknown handler: " action-vec))
     (recur (<! actions-chan))))
