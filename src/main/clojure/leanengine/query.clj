(ns clojure.leanengine.query
  (:import (com.avos.avoscloud AVQuery)))

(defn avos-query
  [^String class-name]
  (AVQuery. class-name))
