(ns web-audio-cljs.state
  (:require [om.core :as om :include-macros true]
            [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [put! chan timeout <! >! onto-chan]]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

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

(defn handle-update-recorded-sound-note-type [app-state recorded-sound-path note-type]
  (let [recorded-sound (get-in @app-state recorded-sound-path)
        i (last recorded-sound-path)
        new-recorded-sound (assoc recorded-sound :current-note-type note-type)]
    (om/transact! app-state :recorded-sounds #(assoc % i new-recorded-sound))))

(defn handle-update-recorded-sound-offset [app-state recorded-sound-path x-offset]
  (let [recorded-sound (get-in @app-state recorded-sound-path)
        i (last recorded-sound-path)
        new-recorded-sound (assoc recorded-sound :current-offset x-offset)]
    (om/transact! app-state :recorded-sounds #(assoc % i new-recorded-sound))))

(defn handle-new-play-sound [app-state recorded-sound-path]
  (let [recorded-sound (get-in @app-state recorded-sound-path)]
    (om/transact! app-state :play-sounds #(conj % (new-play-sound recorded-sound)))))

(defn actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
     (match [action-vec]
       [[:toggle-recording sound-name]] (handle-toggle-recording app-state sound-name)
       [[:set-recorded-sound-note-type recorded-sound-path note-type]]
            (handle-update-recorded-sound-note-type app-state recorded-sound-path note-type)
       [[:set-recorded-sound-offset recorded-sound-path x-offset]]
            (handle-update-recorded-sound-offset app-state recorded-sound-path x-offset)
       [[:new-play-sound recorded-sound-path]] (handle-new-play-sound app-state recorded-sound-path)
       :else (.log js/console "Unknown handler: " (clj->js action-vec)))
     (recur (<! actions-chan))))
