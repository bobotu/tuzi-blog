(ns leanengine.object
  "对 leancloud 云引擎的简单封装"
  (:import (com.avos.avoscloud AVObject AVSaveOption))
  (:use [leanengine.base]))

(defn- handle-value
  "将数据中所有的 sym 和 keyword 转化成 string
   转化后除了 map 之外的 coll 均变为 seq"
  [v]
  (cond
    (or (keyword? v) (symbol? v)) (name v)
    (string? v) v
    (map? v) (apply hash-map (mapcat handle-value v))
    (coll? v) (map handle-value v)
    :else v))

(defn- save-map-flatten
  "将 map 展开为 key-value 对 存储在 leancloud 中"
  [^AVObject avos-object map]
  (let [seq (seq map)]
    (avos-try
      (doseq [[k v] seq]
        (.put avos-object (handle-value k) (handle-value v))))))

(defn- save-key-value
  "将 value 存入 avos-object 的 key 中
   支持 map list set string vec 数据
   其中的 sym 和 keyword 将转化成 string
   keyword 丢弃 namespace"
  [^AVObject avos-object ^String k value]
  (avos-try
    (doto avos-object
      (.put k (handle-value value)))))

(defn- put-kv
  [avos-object k v]
  (cond
    (= :flatten-avos-object k)
    (save-map-flatten avos-object v)
    :else
    (save-key-value avos-object k v)))

(defn avos-object
  [^String class-name]
  ^AVObject (AVObject. class-name))

(defn save-avos-object
  "提供 avos-object [bidings]
   bindings 形式为 [key1 value1 key2 value2] 或者是 一个 map
   key 需为 string
   value 可为 set map list set string vec sym keyword
   value 中所有的 sym keyword 均化为 string
   除了 map 之外的 coll 均变为 seq
   如 key 为 :flatten-avos-object value 为 map 则将 map 的 key value 展开存入 avos-object (只展开一层)
   末尾可用 :save-option ^AVSaveOption option 绑定条件存储"
  [^AVObject avos-object bindings]
  (when (map? bindings) (recur avos-object (mapcat vec bindings)))
  (loop [seq (apply list bindings)]
    (let [[^String k v opt option] seq]
      (cond
        (= opt :save-option)
        (do
          (put-kv avos-object k v)
          (avos-try
            (.save avos-object ^AVSaveOption option)
            (.getObjectId avos-object)))
        (and (nil? opt) (nil? option))
        (do
          (put-kv avos-object k v)
          (avos-try
            (.save avos-object)
            (.getObjectId avos-object)))
        (not-any? nil? [k v opt option])
        (do
          (put-kv avos-object k v)
          (recur (drop 2 seq)))
        :else
        (throw "保存DSL绑定错误!")))))
