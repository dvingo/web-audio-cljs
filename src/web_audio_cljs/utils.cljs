(ns web-audio-cljs.utils
  (:require [clojure.string :as string]))
(defn l [& args] (.log js/console " " (string/join args)))
