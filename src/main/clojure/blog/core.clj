(ns blog.core
  (:use
    [ring.util.servlet :only (defservice)]
    [compojure.core]
    [selmer.parser]
    [selmer.filters]
    [clojure.java.io]
    [ring.util.response]
    [ring.util.codec :as codec]
    [blog.util]
    [medley.core]
    [adapter.leanengine]
    [leanengine.base]
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

(dorun (map #(error % " : " (System/getenv %)) ["LEANCLOUD_APP_ID"
                                                    "LEANCLOUD_APP_ENV"
                                                    "LEANCLOUD_APP_KEY"
                                                    "LEANCLOUD_APP_MASTER_KEY"
                                                    "LEANCLOUD_APP_PORT"]))

(init-avos)

(defn- get-avatar
  [data]
  (assoc data :avatar "http://ac-qlbfmmkg.clouddn.com/62204347ffcbe999.JPG"))

(defn- render-404
  []
  (-> (render-file "404.html" {})
      (response)
      (status 404)
      (content-type "text/html; charset=utf-8")))

(def ^:const list-size 6)

(defroutes
  my-blog
  (GET "/" [] (redirect "/index/1"))
  (GET "/index/:p" [p]
    (let [p (Long/parseLong p)
          posts-data (get-posts-list (* (- p 1) list-size) list-size)
          count (posts-count)]
      (if (empty? posts-data)
        (render-404)
        (-> {:posts posts-data}
            (post-list-summary)
            (get-avatar)
            (navigator-index p count list-size)
            (select-section 1)
            (#(render-file "index.html" %))
            (response)
            (content-type "text/html; charset=utf-8")))))

  (GET "/tags" []
    (let [tags-data (all-tags)]
      (-> {:tags tags-data}
          (get-avatar)
          (select-section 2)
          (#(render-file "tag-list.html" %))
          (response)
          (content-type "text/html; charset=utf-8"))))

  (GET "/tag/:tag" [tag]
    (redirect (str "/tag/" (url-encode tag) "/1")))

  (GET "/tag/:tag/:p" [tag p]
    (let [p (Long/parseLong p)
          posts-data (get-tag-posts-list tag (* (- p 1) list-size) list-size)
          count (tag-posts-count tag)]
      (if (empty? posts-data)
        (render-404)
        (-> {:posts posts-data
             :name  tag
             :count count}
            (get-avatar)
            (post-list-summary)
            (navigator-index p count list-size)
            (select-section 2)
            (#(render-file "tag-detail.html" %))
            (response)
            (content-type "text/html; charset=utf-8")))))

  (GET "/post/:id" [id]
    (let [post-data (get-post-by-id id)]
      (if (empty? post-data)
        (render-404)
        (-> {:post post-data}
            (get-avatar)
            (select-section 1)
            (#(render-file "post-detail.html" %))
            (response)
            (content-type "text/html; charset=utf-8")))))

  (GET "/about" []
    (-> {}
        (get-avatar)
        (select-section 3)
        (#(render-file "about.html" %))
        (response)
        (content-type "text/html; charset=utf-8")))

  (ANY "*" [] (render-404)))

(defservice my-blog)