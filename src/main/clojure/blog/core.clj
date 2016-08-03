(ns blog.core
  (:use
    [ring.util.servlet :only (defservice)]
    [compojure.core])
  (:gen-class
    :extends javax.servlet.http.HttpServlet))

(defroutes
  my-blog
  (ANY "*" [] "<h1>Hello World</h1>"))

(defservice my-blog)