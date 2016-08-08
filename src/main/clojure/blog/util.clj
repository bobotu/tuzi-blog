(ns blog.util
  (:use adapter.leanengine))

(defn post-summary
  [post]
  (assoc post :summary (:content post)))

(defn replace-in
  [coll in f]
  (->> (get-in coll in ())
       (map f)
       (assoc-in coll in)))

(defn post-list-summary
  [posts]
  (replace-in posts [:posts] post-summary))

(defn navigator-index
  [data p max list-size]
  (assoc data :prev (- p 1) :next (if (<= max (* p list-size)) 0 (+ p 1))))

(defn select-section
  [data index]
  (assoc data :section-select index))
