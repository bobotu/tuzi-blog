(ns leanengine.core
  "对 leancloud 云引擎的简单封装"
  (:import (com.avos.avoscloud AVOSCloud AVObject AVQuery AVException)
           (java.util Set Map List))
  (:require [medley.core :as medley]))

(defn init-avos
  [^String app-id ^String app-key ^String master-key]
  (AVOSCloud/initialize app-id app-key master-key))

(defn- avos-error-parse [error] nil)

(defmacro ^:private avos-try
  "将表达式包入 try catch 块中,如发生错误返回错误信息"
  [& body]
  `(try
     ~@body
     (catch AVException e#
       (if-let [info# (avos-error-parse e#)]
         info#
         (.printStackTrace e#)))))

(defn- handle-sym-keyword
  "将数据中所有的 sym 和 keyword 转化成 string
   转化后除了 map 之外的 coll 均变为 seq"
  [v]
  ^Object (cond
            (or (keyword? v) (symbol? v)) (name v)
            (map? v) (apply hash-map (mapcat handle-sym-keyword v))
            (coll? v) (map handle-sym-keyword v)
            :else v))

(defn- save-map-flatten
  "将 map 展开为 key-value 对 存储在 leancloud 中"
  [^AVObject av-object map]
  (let [seq (seq map)]
    (avos-try
      (doseq [[k v] seq]
        (.put av-object (handle-sym-keyword k) (handle-sym-keyword v))))))

(defn- save-key-value
  "将 value 存入 av-object 的 key 中
   支持 map list set string vec 数据
   其中的 sym 和 keyword 将转化成 string
   keyword 丢弃 namespace"
  [^AVObject av-object ^String k ^Object value]
  (avos-try
    (doto av-object
      (.put k (handle-sym-keyword value)))))

(defn save-av-object
  "提供 av-object [bidings]
   bindings 形式为 key value
   key 需为 string
   value 可为 set map list set string vec sym keyword
   value 中所有的 sym keyword 均化为 string
   除了 map 之外的 coll 均变为 seq
   如 key 为 :flatten-av-object value 为 map 则将 map 的 key value 展开存入 av-object (只展开一层)"
  [^AVObject av-object & bindings]
  (loop [seq (seq bindings)]
    (let [[^String k v] seq]
      (cond
        (= :flatten-av-object k)
        (do
          (save-map-flatten av-object v)
          (recur (drop 2 seq)))
        (or (nil? k) (nil? v))
        (avos-try
          (.save av-object))
        :else
        (do
          (save-key-value av-object k v)
          (recur (drop 2 seq)))))))



