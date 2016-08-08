(ns adapter.leanengine
  (:use leanengine.base
        leanengine.query
        leanengine.object
        leanengine.file)
  (:import (com.avos.avoscloud AVObject AVQuery)))

(defn read-post
  "将数据转换为 post 格式"
  [^AVObject object]
  (get-slot object
            ["title" :str
             "date" :date
             "content" :str
             "tags" :seq
             "id" :object-id]))

(defn write-post
  "将 post 转化为数据格式"
  [post]
  (let [object (if (contains? post :id)
                 (avos-object "Post" (:id post))
                 (avos-object "Post"))]
    (-> object
        (put-object ["title" (:title post)
                     "content" (:content post)
                     "tag" (:tag post)]))))

(defn- simple-query
  [class field rel val]
  ^AVQuery (-> (avos-query class)
               (query-object [field rel val])))

(defn add-tag
  "根据 object 的 tag 字段设置关系存储"
  [post tag]
  (-> (avos-query "Tag")
      (query-object ["name" := tag])
      (query-find)
      ((fn [seq] (if-let [t (first seq)]
                   (add-relation t "posts" post)
                   (-> (avos-object "Tag")
                       (put-object ["name" tag])
                       (inc-field "count")
                       (save-object)
                       (add-relation "posts" post)))))))

(defn remove-tag
  "根据 object 的 tag 字段设置关系存储"
  [post tag]
  (-> (avos-query "Tag")
      (query-object ["name" := tag])
      (query-find)
      (first)
      (remove-relation "posts" post)))

(defn get-posts-list
  "查询所有 post 提供 skip 和 limit"
  ([skip limit]
   (get-posts-list (avos-query "Post") skip limit))
  ([^AVQuery query skip limit]
   (-> query
       (query-order [:desc "createdAt"])
       (query-limit limit)
       (query-skip skip)
       (query-find)
       ((partial map read-post)))))

(defn all-tags
  "查询Tag列表"
  []
  (->> (avos-query "Tag")
       (query-find)
       (map #(-> (get-slot % ["name" :str "count" :num])))))

(defn get-tag-posts-list
  [^String tag-name skip limit]
  (->> (-> (avos-query "Tag")
           (query-object ["name" := tag-name])
           (query-find))
       (map #(relation-query % "posts"))
       (mapcat #(get-posts-list % skip limit))))

(defn get-post-by-id
  [^String id]
  (-> (.get ^AVQuery (avos-query "Post") id)
      (read-post)))

(defn posts-count
  []
  (-> (avos-query "Post")
      (query-count)))

(defn tag-posts-count
  [tag-name]
  (-> (avos-query "Tag")
      (query-object ["name" := tag-name])
      (query-find)
      ((partial map #(get-slot % ["count" :num])))
      (first)
      (:count)))