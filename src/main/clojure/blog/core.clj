(ns blog.core
  (:use
    [ring.util.servlet :only (defservice)]
    [compojure.core]
    [compojure.coercions]
    [selmer.parser]
    [selmer.filters]
    [clojure.java.io]
    [ring.util.response]
    [ring.util.codec :as codec]
    [blog.util]
    [adapter.leanengine]
    [clojure.tools.logging])
  (:gen-class
    :extends javax.servlet.http.HttpServlet))

(defn- section-selected-index
  [set-index index]
  (if (= set-index index) "selected" ""))

(set-resource-path! (resource "views"))
(add-filter! :nav-out-of-range? (fn [index] (if (>= 0 index) "hidden" "visible")))
(add-filter! :url-decode (fn [code] (codec/url-decode code)))
(dorun (map #(add-filter! (keyword (str "section-select-" % "?")) (partial section-selected-index %)) (range 1 4)))

(defn- get-avatar
  [data]
  (assoc data :avatar "https://dn-qlbfmmkg.qbox.me/62204347ffcbe999.JPG"))

(def ^:const list-size 6)

(defroutes
  my-blog
  (GET "/" [] (redirect "/index/1" 301))
  (GET "/index/:p" [p :<< as-int]
    (let [posts-data (get-posts-list (* (- p 1) list-size) list-size)
          count (posts-count)]
      (when-not (empty? posts-data)
        (-> {:posts posts-data}
            (handle-summary :posts)
            (get-avatar)
            (navigator-index p count list-size)
            (select-section 1)
            (#(render-file "blog/index.html" %))))))

  (GET "/tags" []
    (let [tags-data (all-tags)]
      (-> {:tags tags-data}
          (get-avatar)
          (select-section 2)
          (#(render-file "blog/tag-list.html" %)))))

  (GET "/tag/:tag" [tag]
    (redirect (str "/tag/" (url-encode tag) "/1") 301))

  (GET "/tag/:tag/:p" [tag p :<< as-int]
    (let [posts-data (get-tag-posts-list tag (* (- p 1) list-size) list-size)
          count (tag-posts-count tag)]
      (when-not (empty? posts-data)
        (-> {:posts posts-data
             :name  tag
             :count count}
            (get-avatar)
            (handle-summary :posts)
            (navigator-index p count list-size)
            (select-section 2)
            (#(render-file "blog/tag-detail.html" %))))))

  (GET "/post/:id" [id]
    (let [post-data (get-post-by-id id)]
      (when-not (empty? post-data)
        (-> {:post post-data}
            (get-avatar)
            (select-section 1)
            (#(render-file "blog/post-detail.html" %))))))

  (GET "/about" []
    (-> {}
        (get-avatar)
        (select-section 3)
        (#(render-file "blog/about.html" %))))

  (POST "/try" [code]
    (if (= code "2713")
      (status 200 (response "haha"))
      (status 400 (response "fuck"))))

  (ANY "*" [] (-> (render-file "404.html" {})
                  (response)
                  (status 404)
                  (content-type "text/html; charset=utf-8"))))

(defservice my-blog)