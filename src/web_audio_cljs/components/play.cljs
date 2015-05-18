(ns web-audio-cljs.components.play
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [web-audio-cljs.state :refer [composition-duration-sec
                                          track-samples
                                          audio-look-ahead-time-sec
                                          track-width
                                          audio-context]]
            [web-audio-cljs.utils :refer [lin-interp play-track-sample!]]))

(defn schedule-samples [t-samples]
  (let [current-time (.-currentTime audio-context)
        track-offset-time (js/Number (.toFixed (- (mod current-time composition-duration-sec) .02) 2))
        px-offset->secs (lin-interp 0 track-width 0 composition-duration-sec)
        samples-time-offset (map #(px-offset->secs (:offset %)) t-samples)
        window-max-time (+ track-offset-time audio-look-ahead-time-sec)
        samples-to-play (filter #(and (>= (px-offset->secs (:offset %))
                                          track-offset-time)
                                      (<= (px-offset->secs (:offset %))
                                          window-max-time))
                                t-samples)]
    (when (> (count samples-to-play) 0)
      (.log js/console "num samples to play: " (count samples-to-play)))
    (doall
      (map (fn [x] (.log js/console "Playing sample! ") (play-track-sample! audio-context x)) samples-to-play))))

(defn play-view [_ owner]
  (reify
    om/IDisplayName (display-name [_] "play-view")
    om/IInitState (init-state [_] {:current-time (.-currentTime audio-context)})
    om/IDidUpdate
    (did-update [_ _ _]
      (om/set-state! owner :current-time (.-currentTime audio-context)))
    om/IRenderState
    (render-state [_ {:keys [current-time]}]
      (let [t-samples (om/observe owner (track-samples))
            track-offset-time (.toFixed (mod current-time composition-duration-sec) 2)]
        (schedule-samples t-samples)
        (dom/div nil track-offset-time)))))
