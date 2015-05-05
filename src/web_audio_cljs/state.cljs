(ns web-audio-cljs.state
  (:require [om.core :as om :include-macros true]
            [cljs-uuid.core :as uuid]
            [cljs.core.async :refer [put! chan timeout <! >! onto-chan]]
            [cljs.core.match :refer-macros [match]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; recorded-sounds
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn new-recorded-sound [audio-buffer aname]
  {:id (uuid/make-random) :name aname :audio-buffer audio-buffer :current-note-type :quarter})

(defn save-recording! [app-state sound-name]
  (let [{:keys [audio-recorder audio-context analyser-node]} @app-state
        buffer-length (.-frequencyBinCount analyser-node)]
    (.stop audio-recorder)
    (.getBuffers audio-recorder
                 (fn [buffers]
                   (om/transact! app-state :recorded-sounds
                                 #(conj % (new-recorded-sound (aget buffers 0) sound-name)))
                   (.clear audio-recorder)))))

(defn handle-toggle-recording [app-state sound-name]
  (let [{:keys [is-recording audio-recorder]} @app-state]
    (if is-recording
      (save-recording! app-state sound-name)
      (.record audio-recorder))
    (om/transact! app-state :is-recording not)))

(defn indices-of [f coll]
  (keep-indexed #(if (f %2) %1 nil) coll))

(defn first-index-of [f coll]
  (first (indices-of f coll)))

(defn find-in [value coll]
  (first-index-of #(= % value) coll))

(defn handle-update-recorded-sound-note-type [app-state recorded-sound-id note-type]
  (.log js/console "got recorded sound id: " recorded-sound-id)
  (.log js/console "got note-type: " note-type)
  (let [recorded-sounds (:recorded-sounds @app-state)
        ids (vec (map #(:id %) recorded-sounds))
        i (find-in recorded-sound-id ids)
        recorded-sound (get recorded-sounds i)
        new-recorded-sound (assoc recorded-sound :current-note-type note-type)]
    (om/transact! app-state :recorded-sounds #(assoc % i new-recorded-sound))))

(defn actions-handler [actions-chan app-state]
  (go-loop [action-vec (<! actions-chan)]
     (match [action-vec]
       [[:toggle-recording sound-name]] (handle-toggle-recording app-state sound-name)
       [[:set-recorded-sound-note-type recorded-sound-id note-type]]
            (handle-update-recorded-sound-note-type app-state recorded-sound-id note-type)
       :else (.log js/console "Unknown handler: " action-vec))
     (recur (<! actions-chan))))
