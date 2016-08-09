(ns blog.init
  (:gen-class :implements [javax.servlet.ServletContextListener])
  (:import (javax.servlet ServletContextEvent)
           (cn.leancloud LeanEngine)))

(defn -contextInitialized
  [this arg1]
  (LeanEngine/initialize
    (System/getenv "LEANCLOUD_APP_ID")
    (System/getenv "LEANCLOUD_APP_KEY")
    (System/getenv "LEANCLOUD_APP_MASTER_KEY")))

(defn -contextDestroyed [this arg1] )