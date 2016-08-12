(ns adapter.leanengine
  (:use leanengine.base
        leanengine.query
        leanengine.object
        leanengine.file
        clojure.tools.logging
        clojure.set)
  (:import (com.avos.avoscloud AVObject AVQuery AVUser AVFile AVException)))

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
  ([^String class post]
   (let [object (if (:id post)
                  (avos-object class (:id post))
                  (avos-object class))]
     (-> object
         (put-object ["title" (:title post)
                      "content" (:content post)
                      "tags" (get post :tags [])])))))

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
                       (save-object)
                       (inc-field "count")
                       (add-relation "posts" post)))))))

(defn remove-tag
  "根据 object 的 tag 字段设置关系存储"
  [post tag]
  (-> (avos-query "Tag")
      (query-object ["name" := tag])
      (query-find)
      (first)
      (dec-field "count")
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

(defn avos-file-store
  [{:keys [filename content-type bytes]}]
  (save-avos-file filename bytes))

(defn save-draft
  [post]
  (try
    (-> (write-post "Draft" post)
        (save-object)
        (get-slot ["id" :object-id])
        (assoc :success? true))
    (catch AVException e
      {:success? false :error (.getMessage e)})))

(defn- handle-tag
  [old new post]
  (let [new (set new)
        old (set old)
        added (difference new old)
        deleted (difference old new)
        add-task (map (partial add-tag post) added)
        deleted-task (map (partial remove-tag post) deleted)]
    (save-objects (concat add-task deleted-task))
    post))

(defn- handle-post
  [post draft]
  (-> post
      ((partial write-post "Post"))
      (put-object ["tags" (get draft :tags)])
      (save-object)
      ((partial handle-tag (get post :tags) (get draft :tags)))))

(defn save-post
  [draft post]
  (try
    (let [post (handle-post post draft)
          {id :id} (get-slot post ["id" :object-id])
          draft (write-post "Draft" draft)]
      (-> (put-object draft ["post" post
                             "changed" false])
          (save-object)
          (get-slot ["draftID" :object-id])
          (assoc :postID id :success? true)))
    (catch AVException e
      {:success? false :error (.getMessage e)})))

(defn save-darf-img
  [draft-id file]
  (-> (avos-object "Draft" draft-id)
      (array-add "image" file)
      (save-object))
  {
   :id  (.getObjectId file)
   :url (.getUrl file)
   })

(defn new-draft
  []
  (-> (avos-object "Draft")
      (save-object)
      (get-slot ["id" :object-id])))

(defn read-draft
  [object]
  (get-slot object ["id" :object-id
                    "tags" :seq
                    "date" :date
                    "content" :str
                    "title" :str
                    "changed" :bool]))

(defn get-draft-by-id
  [id]
  ())

(defn drafts-count
  []
  (-> (avos-query "Draft")
      (query-count)))

(defn get-drafts-list
  [skip limt]
  (-> (avos-query "Draft")
      (query-order [:desc "createdAt"])
      (query-limit limt)
      (query-skip skip)
      (query-find)
      ((partial map read-draft))))

(defn parse-draft-info
  [draft-id]
  (let [draft (-> (avos-query "Draft")
                  (query-include "image")
                  (query-include "post")
                  (.get draft-id))
        draft-info (read-draft draft)
        post (.getAVObject draft "post")
        post-info (if (nil? post)
                    {:id nil :tags []}
                    (get-slot post ["id" :object-id
                                    "tags" :seq]))
        imgs (:image (get-slot draft ["image" :seq]))]
    {:draftID (:id draft-info)
     :postID  (:id post-info)
     :oldTags (:tags post-info)
     :newTags (:tags draft-info)
     :title   (:title draft-info)
     :content (:content draft-info)
     :images  (map (fn [el] {:url (el "url") :id (el "objectId")}) imgs)}))