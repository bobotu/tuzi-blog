(ns blog.util
  (:use adapter.leanengine
        compojure.core)
  (:import org.jsoup.Jsoup))

(defn- parse-summary
  [data]
  (assoc data :summary (->> (.text (Jsoup/parse (:content data)))
                            (take 300)
                            (apply str))))

(defn replace-in
  [coll in f]
  (->> (get-in coll in ())
       (map f)
       (assoc-in coll in)))

(defn handle-summary
  [data path]
  (replace-in data [path] parse-summary))

(defn navigator-index
  [data p max list-size]
  (assoc data :prev (- p 1) :next (if (<= max (* p list-size)) 0 (+ p 1))))

(defn select-section
  [data index]
  (assoc data :section-select index))

(defn wrap-check-logedin
  [handler]
  (fn [request]
    ))