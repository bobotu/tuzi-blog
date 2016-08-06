(ns adapter.test
  (:use adapter.leanengine
        leanengine.object
        leanengine.base
        leanengine.query
        clojure.test))

(deftest
  leancloud-init-test
  (init-avos
    "qLbFMmKGA4KW18gopi88gTxe-gzGzoHsz"
    "wWJGHMNLMwy5SnANaurrUdh8"
    "yRKW7uTJHTgzKVRfmPcWvddE"))

