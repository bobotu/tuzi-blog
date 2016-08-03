(ns blog.core
  (:use
    [ring.util.servlet :only (defservice)]
    [compojure.core])
  (:gen-class
    :extends javax.servlet.http.HttpServlet))

(defroutes
  my-blog
  (GET "/" [] "<h1>Hello World</h1>")
  (ANY "*" [] "<h1>Hello World 1</h1>"))

(defservice my-blog)