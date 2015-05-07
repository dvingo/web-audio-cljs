(ns web-audio-cljs.state
  (:require [om.core :as om :include-macros true]
            [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [put! chan timeout <! >! onto-chan]]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(let [db {:compositions []
          :tracks []
          :recorded-sounds []
          :play-sounds []
          :analyser-node nil
          :audio-recorder nil
          :is-recording false
          :bpm 120}]

  (defonce app-state (atom db)))

(defonce audio-context (js/window.AudioContext.))

(defn play-sounds []
  (om/ref-cursor (:play-sounds (om/root-cursor app-state))))

(defn recorded-sounds []
  (om/ref-cursor (:recorded-sounds (om/root-cursor app-state))))

(defn new-recorded-sound [audio-buffer aname]
  {:id (uuid/make-random)
   :name aname
   :audio-buffer audio-buffer
   :current-offset 0
   :current-note-type :quarter})

(defn save-recording! [app-state sound-name]
  (let [{:keys [audio-recorder analyser-node]} @app-state
        buffer-length (.-frequencyBinCount analyser-node)]
    (.stop audio-recorder)
    (.getBuffers audio-recorder
                 (fn [buffers]
                   (om/transact! app-state :recorded-sounds
                                 #(conj % (new-recorded-sound (aget buffers 0) sound-name)))
                   (.clear audio-recorder)))))

(defn new-play-sound [recorded-sound]
  {:id (uuid/make-random)
   :name (:name recorded-sound)
   :audio-buffer (:audio-buffer recorded-sound)
   :offset (:current-offset recorded-sound)
   :type (:current-note-type recorded-sound)
   :recorded-sound (:id recorded-sound)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-toggle-recording [app-state sound-name]
  (let [{:keys [is-recording audio-recorder]} @app-state]
    (if is-recording
      (save-recording! app-state sound-name)
      (.record audio-recorder))
    (om/transact! app-state :is-recording not)))

(defn handle-update-recorded-sound-note-type [app-state recorded-sound note-type]
  (let [i (last (om/path recorded-sound))
        new-recorded-sound (assoc recorded-sound :current-note-type note-type)]
    (om/transact! (recorded-sounds) #(assoc % i new-recorded-sound))))

(defn handle-update-recorded-sound-offset [rec-sound-idx x-offset]
  (let [rec-sounds (recorded-sounds)
        new-recorded-sound (assoc (get rec-sounds rec-sound-idx) :current-offset x-offset)]
    (om/transact! rec-sounds #(assoc % rec-sound-idx new-recorded-sound))))

(defn handle-new-play-sound [app-state recorded-sound]
  (om/transact! (play-sounds) #(conj % (new-play-sound recorded-sound))))

(defn actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
     (match [action-vec]
       [[:toggle-recording sound-name]] (handle-toggle-recording app-state sound-name)
       [[:set-recorded-sound-note-type recorded-sound note-type]]
            (handle-update-recorded-sound-note-type app-state recorded-sound note-type)
       [[:set-recorded-sound-offset rec-sound-index x-offset]]
            (handle-update-recorded-sound-offset rec-sound-index x-offset)
       [[:new-play-sound recorded-sound]] (handle-new-play-sound app-state recorded-sound)
       :else (.log js/console "Unknown handler: " (clj->js action-vec)))
     (recur (<! actions-chan))))
