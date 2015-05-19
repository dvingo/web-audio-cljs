(ns web-audio-cljs.components.play
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [composition-duration-sec
                                          track-samples
                                          audio-look-ahead-time-sec
                                          track-width
                                          audio-context
                                          sixteenth-note-length]]
            [web-audio-cljs.utils :refer [lin-interp play-track-sample!]])
  (:require-macros [web-audio-cljs.macros :refer [send!!]]))

(defn schedule-samples [t-samples]
  (let [current-time (.-currentTime audio-context)
        track-offset-time (mod current-time composition-duration-sec)
        px-offset->secs (lin-interp 0 track-width 0 composition-duration-sec)
        ;;samples-time-offset (map #(px-offset->secs (:offset %)) t-samples)
        window-max-time (mod (+ track-offset-time audio-look-ahead-time-sec) composition-duration-sec)
        min-time (- window-max-time audio-look-ahead-time-sec)
        ;; We use (- window-max-time audio-look-ahead-time-sec) to account for
        ;; when the number "wrap" due to modulus - the case where >= 0 and the min time is 9.8 for example.
        track-samples-to-play (filter #(and (>= (px-offset->secs (:offset %)) min-time)
                                            (<= (px-offset->secs (:offset %)) window-max-time))
                                      t-samples)]
    (doseq [s track-samples-to-play]
      (play-track-sample! audio-context s))))

(defonce next-note-time (atom 0.0))

(defn play-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "play-view")
    om/IInitState (init-state [_]
                    {:current-time (.-currentTime audio-context)})
    om/IDidMount (did-mount [_]
                   (om/set-state! owner :current-time (.-currentTime audio-context)))
    om/IDidUpdate (did-update [_ _ _]
      (let [current-time (.-currentTime audio-context)
            t-samples (om/observe owner (track-samples))]
        (while (< @next-note-time (+ current-time audio-look-ahead-time-sec))
          (reset! next-note-time (+ @next-note-time sixteenth-note-length))
          (schedule-samples t-samples))
        (om/set-state! owner :current-time (.-currentTime audio-context))))

    om/IRenderState (render-state [_ {:keys [next-note-time]}]
      (let [current-time (.-currentTime audio-context)
            t-samples (om/observe owner (track-samples))
            track-offset-time (.toFixed (mod current-time composition-duration-sec) 2)]
        (dom/div nil track-offset-time)))))
