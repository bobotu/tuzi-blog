(ns leanengine.file
  (:import (com.avos.avoscloud AVFile))
  (:use leanengine.base))

(defn save-avos-file
  [^String name ^bytes byte]
  ((future
     (doto (AVFile. name byte))
     (.save))))