#!/bin/bash -
lein clean && lein cljsbuild once min
cp resources/public/js/compiled/web_audio_cljs.js ../gh-pages-audio-cljs/js/compiled/web_audio_cljs.js
cp resources/public/css/style.css ../gh-pages-audio-cljs/css/style.css
