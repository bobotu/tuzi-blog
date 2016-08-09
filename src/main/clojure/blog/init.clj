(ns blog.init
  (:gen-class :implements [javax.servlet.ServletContextListener])
  (:import (cn.leancloud LeanEngine)
           (com.avos.avoscloud AVOSCloud)))

(defn -contextInitialized
  [this arg1]
  (AVOSCloud/useAVCloudCN)
  (AVOSCloud/initialize
    (System/getenv "LEANCLOUD_APP_ID")
    (System/getenv "LEANCLOUD_APP_KEY")
    (System/getenv "LEANCLOUD_APP_MASTER_KEY")))

(defn -contextDestroyed [this arg1])