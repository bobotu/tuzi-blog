(ns leanengine.file
  (:import (com.avos.avoscloud AVFile))
  (:use leanengine.base))

(defn save-avos-file
  [^String name ^bytes byte]
  ^AVFile (let [file (AVFile. name byte)]
    (.save file)
    file))