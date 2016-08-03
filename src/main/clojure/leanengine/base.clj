(ns leanengine.base
  (:import (com.avos.avoscloud AVOSCloud AVException)))

(defn init-avos
  [^String app-id ^String app-key ^String master-key]
  (AVOSCloud/initialize app-id app-key master-key))

(defn- avos-error-parse [error] nil)

(defmacro avos-try
  "将表达式包入 try catch 块中,如发生错误返回错误信息"
  [& body]
  `(try
     ~@body
     (catch AVException e#
       (if-let [info# (avos-error-parse e#)]
         info#
         (.printStackTrace e#)))))