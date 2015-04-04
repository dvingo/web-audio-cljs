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

(defn got-stream [stream]
  (l "got hte stream: " stream)
  (let [input-point (.createGain audio-context)
        audio-input (.createMediaStreamSource audio-context stream)]
    (.connect audio-input input-point)
    (let [analyser-node (.createAnalyser audio-context)]
      (set! (.-fftSize analyser-node) 2048)
      (.connect input-point analyser-node))
    (def audio-recorder (js/Recorder. input-point))
    (let [zero-gain (.createGain audio-context)]
      (set! (-> zero-gain .-gain .-value) 0.0)
      (.connect input-point zero-gain)
      (.connect zero-gain  (.-destination audio-context))
    (l "Would coll update analysers here"))))
    ;(updateAnalysers)))


(defn init-audio []
  (when-not (.-getUserMedia js/navigator)
    (set! (.-getUserMedia js/navigator)
          (first (filter #(not (nil? %))
                         [(.-webkitGetUserMedia js/navigator)
                          (.-mozGetUserMedia js/navigator)]))))
  (when-not (.-cancelAnimationFrame js/window)
    (set! (.-cancelAnimationFrame js/window)
          (first (filter #(not (nil? %))
                         [(.-webkitCancelAnimationFrame  js/window)
                          (.-mozCancelAnimationFrame js/window)]))))
  (when-not (.-requestAnimationFrame js/window)
    (set! (.-requestAnimationFrame js/window)
          (first (filter #(not (nil? %))
                         [(.-webkitRequestAnimationFrame  js/window)
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
  )
(init-audio)
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
;; function convertToMono( input ) {
;;     var splitter = audioContext.createChannelSplitter(2);
;;     var merger = audioContext.createChannelMerger(2);
;;
;;     input.connect( splitter );
;;     splitter.connect( merger, 0, 0 );
;;     splitter.connect( merger, 0, 1 );
;;     return merger;
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
;; function toggleMono() {
;;     if (audioInput != realAudioInput) {
;;         audioInput.disconnect();
;;         realAudioInput.disconnect();
;;         audioInput = realAudioInput;
;;     } else {
;;         realAudioInput.disconnect();
;;         audioInput = convertToMono( realAudioInput );
;;     }
;;
;;     audioInput.connect(inputPoint);
;; }
;;
;; function gotStream(stream) {
;;     inputPoint = audioContext.createGain();
;;
;;     // Create an AudioNode from the stream.
;;     realAudioInput = audioContext.createMediaStreamSource(stream);
;;     audioInput = realAudioInput;
;;     audioInput.connect(inputPoint);
;;
;; //    audioInput = convertToMono( input );
;;
;;     analyserNode = audioContext.createAnalyser();
;;     analyserNode.fftSize = 2048;
;;     inputPoint.connect( analyserNode );
;;
;;     audioRecorder = new Recorder( inputPoint );
;;
;;     zeroGain = audioContext.createGain();
;;     zeroGain.gain.value = 0.0;
;;     inputPoint.connect( zeroGain );
;;     zeroGain.connect( audioContext.destination );
;;     updateAnalysers();
;; }
;;
;; function initAudio() {
;;         if (!navigator.getUserMedia)
;;             navigator.getUserMedia = navigator.webkitGetUserMedia || navigator.mozGetUserMedia;
;;         if (!navigator.cancelAnimationFrame)
;;             navigator.cancelAnimationFrame = navigator.webkitCancelAnimationFrame || navigator.mozCancelAnimationFrame;
;;         if (!navigator.requestAnimationFrame)
;;             navigator.requestAnimationFrame = navigator.webkitRequestAnimationFrame || navigator.mozRequestAnimationFrame;
;;
;;     navigator.getUserMedia(
;;         {
;;             "audio": {
;;                 "mandatory": {
;;                     "googEchoCancellation": "false",
;;                     "googAutoGainControl": "false",
;;                     "googNoiseSuppression": "false",
;;                     "googHighpassFilter": "false"
;;                 },
;;                 "optional": []
;;             },
;;         }, gotStream, function(e) {
;;             alert('Error getting audio');
;;             console.log(e);
;;         });
;; }
;;
;; window.addEventListener('load', initAudio );
