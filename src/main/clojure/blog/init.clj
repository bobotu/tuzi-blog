(ns blog.init
  (:gen-class :implements [javax.servlet.ServletContextListener])
  (:import (com.avos.avoscloud AVOSCloud)
           (cn.leancloud LeanEngine EngineSessionCookie)))

(defn -contextInitialized
  [this arg1]
  (AVOSCloud/useAVCloudCN)
  (AVOSCloud/initialize
    (System/getenv "LEANCLOUD_APP_ID")
    (System/getenv "LEANCLOUD_APP_KEY")
    (System/getenv "LEANCLOUD_APP_MASTER_KEY"))
  (LeanEngine/addSessionCookie (EngineSessionCookie. "my-blog~~~" 3600000 false))
  (LeanEngine/setHttpsRedirectEnabled true))

(defn -contextDestroyed [this arg1])