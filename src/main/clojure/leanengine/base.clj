(ns leanengine.base
  (:import (com.avos.avoscloud AVOSCloud AVException)
           (java.lang.reflect Field)))

(defn init-avos
  [^String app-id ^String app-key ^String master-key]
  (AVOSCloud/initialize app-id app-key master-key)
  (AVOSCloud/useAVCloudCN))

(def ^:private error-fields
  (->> (.getFields AVException)
       (filter (fn [^Field f] (= "int" (str (.getType f)))))
       (map (fn [^Field f] [(.getInt f AVException) (.getName f)]))
       (into {})))

(defn avos-error-parse
  [^AVException error]
  (if-let [desc (error-fields (.getCode error))]
    (println (str "https://leancloud.cn/api-docs/android/com/avos/avoscloud/AVException.html#" desc))))

(defmacro avos-try
  "将表达式包入 try catch 块中,如发生错误返回错误信息"
  [& body]
  `(try
     ~@body
     (catch AVException e#
       (if-let [info# (avos-error-parse e#)]
         info#
         (.printStackTrace e#)))))