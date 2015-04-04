(ns web-audio-cljs.audio
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]))

;;
;; Determine which browser audio context to use.
;;
(set! (.-AudioContext js/window)
      (cond (not (nil? (.-AudioContext js/window)))
            (.-AudioContext js/window)
            (not (nil? (.-webkitAudioContext js/window)))
            (.-webkitAudioContext js/window)))
(let [AudioContext (.-AudioContext js/window)]
   (def audio-context (AudioContext.)))

(defn l [& args] (.log js/console " " (string/join args)))

(l "audio context: " audio-context)
(declare n analyser-node)
(set! n 0)
(def canvas (.getElementById js/document "display"))
(def canvas-context (.getContext canvas "2d"))
(def canvas-width (.-width canvas))
(def canvas-height (.-height canvas))
(def spacing 3)
(def bar-width 1)
(def num-bars (.round js/Math (/ canvas-width spacing)))
(l "canvas width: " canvas-width)
(l "canvas height: " canvas-height)

(defn log-data []
  (let [Uint8Array (.-Uint8Array js/window)
        freq-bin-count (.-frequencyBinCount analyser-node)
        freq-byte-data  (Uint8Array. freq-bin-count)]
    (l "called log-data")
    (set! n (inc n))
    (l "freq-byte-data: " freq-byte-data)
    (.getByteFrequencyData analyser-node freq-byte-data)
    (.clearRect canvas-context 0 0 canvas-width canvas-height)
    (set! (.-fillStyle canvas-context) "#F6D565")
    (set! (.-lineCap canvas-context) "round")
    (let [multiplier (/ (.-frequencyBinCount analyser-node) num-bars)]
      (l "multipler: " multiplier)
      (doseq [i (range num-bars)]
        (let [offset (.floor js/Math (* i multiplier))
              magnitude (/ (reduce #(+ (aget freq-byte-data %2) %1) (range multiplier))
                           multiplier)
              magnitude2 (aget freq-byte-data (* i multiplier))]
          (set! (.-fillStyle canvas-context) (str "hsl( " (.round js/Math (/ (* i 360) num-bars)) ", 100%, 50%)"))
          (.fillRect canvas-context (* i spacing) canvas-height bar-width (- magnitude))
          ;(prn "i: " i)
          ;(prn "magnitude: " magnitude)
          ))
      )
    (l "GOT DATA: " (aget freq-byte-data 0)))
    ;(if (< n 100) (.requestAnimationFrame js/window log-data)))
    (.requestAnimationFrame js/window log-data))

(defn got-stream [stream]
  (l "got hte stream: " stream)
  (let [input-point (.createGain audio-context)
        audio-input (.createMediaStreamSource audio-context stream)]
    (.connect audio-input input-point)
    (set! analyser-node (.createAnalyser audio-context))
    (set! (.-fftSize analyser-node) 2048)
    (.connect input-point analyser-node)
    (def audio-recorder (js/Recorder. input-point))
    (let [zero-gain (.createGain audio-context)]
      (set! (-> zero-gain .-gain .-value) 0.0)
      (.connect input-point zero-gain)
      (.connect zero-gain  (.-destination audio-context)))

    (l "freq bin count: " (.-frequencyBinCount analyser-node))
    (log-data)))

(when-not (.-getUserMedia js/navigator)
  (set! (.-getUserMedia js/navigator)
        (first (filter #(not (nil? %))
                       [(.-webkitGetUserMedia js/navigator)
                        (.-mozGetUserMedia js/navigator)]))))

(when-not (.-cancelAnimationFrame js/window)
  (set! (.-cancelAnimationFrame js/window)
        (first (filter #(not (nil? %))
                       [(.-webkitCancelAnimationFrame js/window)
                        (.-mozCancelAnimationFrame js/window)]))))
(when-not (.-requestAnimationFrame js/window)
  (set! (.-requestAnimationFrame js/window)
        (first (filter #(not (nil? %))
                       [(.-webkitRequestAnimationFrame js/window)
                        (.-mozRequestAnimationFrame js/window)]))))
(let [audio-constraints (clj->js
                          {"audio"
                           {"mandatory"
                            {"googEchoCancellation" "false"
                             "googAutoGainControl"  "false"
                             "googNoiseSuppression" "false"
                             "googHighpassFilter"   "false"}
                            "optional" []}})]
  (.getUserMedia js/navigator audio-constraints got-stream #(.log js/console "ERROR getting user media")))
;(declare audio-recorder)

;(defn got-buffers [buffers]
 ;(let [canvas (. js/document (getElementById "wavedisaply"))]
   ;(draw-buffer (.width canvas) (.height canvas)
                ;(.getContext canvas "2d" (first buffers)))
   ;(.exportWAV audioRecorder)))

;(defn draw-buffer
  ;[width height context data]
  ;(let [step (.ceil js/Math
                    ;(/ (.length data)
                       ;width))
        ;amp (/ height 2)]
    ;(set! (.fillStyle context) "silver")
    ;(.log js/console "Got data: " data)))

;(defn init-audio []
  ;(cond (nil? (.getUserMedia navigator))
        ;(se)))



;function initAudio() {
        ;if (!navigator.getUserMedia)
            ;navigator.getUserMedia = navigator.webkitGetUserMedia || navigator.mozGetUserMedia;
        ;if (!navigator.cancelAnimationFrame)
            ;navigator.cancelAnimationFrame = navigator.webkitCancelAnimationFrame || navigator.mozCancelAnimationFrame;
        ;if (!navigator.requestAnimationFrame)
            ;navigator.requestAnimationFrame = navigator.webkitRequestAnimationFrame || navigator.mozRequestAnimationFrame;

    ;navigator.getUserMedia(
        ;{
            ;"audio": {
                ;"mandatory": {
                    ;"googEchoCancellation": "false",
                    ;"googAutoGainControl": "false",
                    ;"googNoiseSuppression": "false",
                    ;"googHighpassFilter": "false"
                ;},
                ;"optional": []
            ;},
        ;}, gotStream, function(e) {
            ;alert('Error getting audio');
            ;console.log(e);
        ;});
;}

;; var audioContext = new AudioContext();
;; var audioInput = null,
;;     realAudioInput = null,
;;     inputPoint = null,
;;     audioRecorder = null;
;; var rafID = null;
;; var analyserContext = null;
;; var canvasWidth, canvasHeight;
;; var recIndex = 0;
;;
;; /* TODO:
;;
;; - offer mono option
;; - "Monitor input" switch
;; */
;;
;; function saveAudio() {
;;     audioRecorder.exportWAV( doneEncoding );
;;     // could get mono instead by saying
;;     // audioRecorder.exportMonoWAV( doneEncoding );
;; }
;;
;; function gotBuffers( buffers ) {
;;     var canvas = document.getElementById( "wavedisplay" );
;;
;;     drawBuffer( canvas.width, canvas.height, canvas.getContext('2d'), buffers[0] );
;;
;;     // the ONLY time gotBuffers is called is right after a new recording is completed -
;;     // so here's where we should set up the download.
;;     audioRecorder.exportWAV( doneEncoding );
;; }
;;
;; function doneEncoding( blob ) {
;;     Recorder.setupDownload( blob, "myRecording" + ((recIndex<10)?"0":"") + recIndex + ".wav" );
;;     recIndex++;
;; }
;;
;; function toggleRecording( e ) {
;;     if (e.classList.contains("recording")) {
;;         // stop recording
;;         audioRecorder.stop();
;;         e.classList.remove("recording");
;;         audioRecorder.getBuffers( gotBuffers );
;;     } else {
;;         // start recording
;;         if (!audioRecorder)
;;             return;
;;         e.classList.add("recording");
;;         audioRecorder.clear();
;;         audioRecorder.record();
;;     }
;; }
;;
;; function cancelAnalyserUpdates() {
;;     window.cancelAnimationFrame( rafID );
;;     rafID = null;
;; }
;;
;; function updateAnalysers(time) {
;;     if (!analyserContext) {
;;         var canvas = document.getElementById("analyser");
;;         canvasWidth = canvas.width;
;;         canvasHeight = canvas.height;
;;         analyserContext = canvas.getContext('2d');
;;     }
;;
;;     // analyzer draw code here
;;     {
;;         var SPACING = 3;
;;         var BAR_WIDTH = 1;
;;         var numBars = Math.round(canvasWidth / SPACING);
;;         var freqByteData = new Uint8Array(analyserNode.frequencyBinCount);
;;
;;         analyserNode.getByteFrequencyData(freqByteData);
;;
;;         analyserContext.clearRect(0, 0, canvasWidth, canvasHeight);
;;         analyserContext.fillStyle = '#F6D565';
;;         analyserContext.lineCap = 'round';
;;         var multiplier = analyserNode.frequencyBinCount / numBars;
;;
;;         // Draw rectangle for each frequency bin.
;;         for (var i = 0; i < numBars; ++i) {
;;             var magnitude = 0;
;;             var offset = Math.floor( i * multiplier );
;;             // gotta sum/average the block, or we miss narrow-bandwidth spikes
;;             for (var j = 0; j< multiplier; j++)
;;                 magnitude += freqByteData[offset + j];
;;             magnitude = magnitude / multiplier;
;;             var magnitude2 = freqByteData[i * multiplier];
;;             analyserContext.fillStyle = "hsl( " + Math.round((i*360)/numBars) + ", 100%, 50%)";
;;             analyserContext.fillRect(i * SPACING, canvasHeight, BAR_WIDTH, -magnitude);
;;         }
;;     }
;;
;;     rafID = window.requestAnimationFrame( updateAnalysers );
;; }
;;
