# Web audio from cljs

A sample project figuring out how to work with web audio, visualizing to a canvas element
and recording a wave file – with the structure of Om.

The repository was created using the following template:

```bash
lein new figwheel <project-name> -- --om
```

To build and run the project:
```bash
rlwrap lein figwheel
```

## Prod build

Make sure you add any public names you don't want minified to externs.js.

In the `master` branch, from the top directory of the project:
```bash
lein clean && lein cljsbuild once min
git checkout gh-pages
mv resources/public/js/compiled/web_audio_cljs.js js/compiled/web_audio_cljs.js
mv resources/public/css/style.css css/style.css
git add js/compiled/web_audio_cljs.js
git commit -m "Message"
git push origin gh-pages
git checkout master
```

# Licensing
This project is licensed under the MIT License:

http://opensource.org/licenses/MIT

## Recorder.js

This project makes use of:

https://github.com/mattdiamond/Recorderjs

## Icons

This project uses the following images:

A record image by Juan Pablo Bravo
- https://thenounproject.com/bravo/
- https://thenounproject.com/term/record/23527/

A mic image by
- Dmitry Baranovskiy
- https://thenounproject.com/term/microphone/5027/

## Button Styles
Codrops article "Inspiration for Button Styles and Effects"
http://tympanus.net/codrops/2015/02/26/inspiration-button-styles-effects/
