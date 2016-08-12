(ns adapter.test
  (:use adapter.leanengine
        leanengine.object
        leanengine.base
        leanengine.query
        clojure.test))

(deftest
  leancloud-init-test
  (init-avos))

