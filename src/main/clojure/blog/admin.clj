(ns blog.admin
  (:use
    [ring.util.servlet :only (defservice)]
    [compojure.core]
    [compojure.coercions]
    [selmer.parser]
    [clojure.java.io]
    [ring.util.response]
    [ring.middleware.multipart-params]
    [ring.middleware.multipart-params.byte-array]
    [ring.middleware.params]
    [ring.middleware.json]
    [ring.middleware.keyword-params]
    [medley.core :only (map-keys)]
    [blog.util]
    [adapter.leanengine]
    [clojure.tools.logging]
    [clojure.data.json])
  (:import [com.avos.avoscloud AVUser AVException])
  (:gen-class
    :extends javax.servlet.http.HttpServlet))

(set-resource-path! (resource "views"))

(def ^:const list-size 6)

(defn- wrap-login
  [handler]
  (fn [request]
    (if (nil? (AVUser/getCurrentUser))
      (redirect "/admin/login" 301)
      (handler request))))

(defmacro check-login
  [& body]
  `(if (nil? (AVUser/getCurrentUser))
     (status (response {:success? false}) 401)
     (do ~@body)))

(defroutes
  my-blog-admin
  (context "/admin" []

    (-> (GET "/" [] (redirect "/admin/list/1" 301))
        (wrap-routes wrap-login))

    (GET "/login" []
      (-> (render-file "admin/login.html" {})))

    (-> (POST "/login" [username password]
          (if (or (nil? username) (nil? password))
            (do
              (info (str username " " password))
              (render-file "admin/login.html" {:tip "帐号密码没填吧?"}))
            (try
              (AVUser/logIn ^String username ^String password)
              (redirect "/admin/" 301)
              (catch AVException e
                (render-file "admin/login.html" {:tip (.getMessage e)})))))
        (wrap-routes wrap-params))

    (GET "/logout" []
      (AVUser/logOut)
      (redirect "/admin/login" 301))

    (-> (GET "/new" []
          (render-file "admin/editor.html" {}))
        (wrap-routes wrap-login))

    (-> (POST "/get-draft-id" []
          (check-login
            (response (new-draft))))
        (wrap-routes wrap-json-response))

    (-> (POST "/save-draft" [:as {draft :json-params}]
          (check-login
            (response (save-draft (map-keys keyword draft)))))
        (wrap-routes (comp wrap-json-response
                           wrap-json-params)))

    (-> (POST "/save-post" [draft post]
          (check-login
            (response (save-post (map-keys keyword draft)
                                 (map-keys keyword post)))))
        (wrap-routes (comp wrap-json-response
                           wrap-json-params
                           wrap-keyword-params)))

    (-> (POST "/upload-img" [upload-file draft-id]
          (-> (save-darf-img draft-id upload-file)
              (assoc :success? true)
              (response)
              (status 201)))
        (wrap-routes (comp wrap-login
                           (fn [handler] (wrap-multipart-params handler {:store (comp avos-file-store (byte-array-store))}))
                           wrap-keyword-params
                           wrap-json-response)))

    (-> (GET "/list/:p" [p :<< as-int]
          (let [drafts-data (get-drafts-list (* (- p 1) list-size) list-size)
                count (drafts-count)]
            (-> {:drafts drafts-data}
                (handle-summary :drafts)
                (navigator-index p count list-size)
                (#(render-file "admin/list.html" %)))))
        (wrap-routes wrap-login))

    (-> (GET "/edit/:id" [id]
          (try
            (let [data (write-str (parse-draft-info id))]
              (-> (render-file "admin/editor.html" {:draft-info data})
                  (response)
                  (content-type "text/html; charset=utf-8")))
            (catch AVException e
              (if (= (.getCode e) (AVException/OBJECT_NOT_FOUND))
                (-> (render-file "404.html" {})
                    (response)
                    (status 404))))))
        (wrap-routes wrap-login))))

(defservice my-blog-admin)