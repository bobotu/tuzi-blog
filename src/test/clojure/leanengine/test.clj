(ns leanengine.test
  (:use leanengine.object
        leanengine.base
        leanengine.query
        clojure.test)
  (:import (com.avos.avoscloud AVObject AVQuery AVOSCloud)))

(deftest
  handle-value-test
  (is (map? (#'leanengine.object/handle-value {:a 1})))
  (is (seq? (#'leanengine.object/handle-value [1 2 3])))
  (is (seq? (#'leanengine.object/handle-value '(1 2 3))))
  (is (seq? (#'leanengine.object/handle-value (seq [1 2 3]))))
  (is (string? (#'leanengine.object/handle-value 'a)))
  (is (string? (#'leanengine.object/handle-value "a")))
  (is (string? (#'leanengine.object/handle-value :a)))
  (is (instance? AVObject (#'leanengine.object/handle-value (avos-object "Test")))))

(deftest
  leancloud-init-test
  (AVOSCloud/initialize
    (System/getenv "LEANCLOUD_APP_ID")
    (System/getenv "LEANCLOUD_APP_KEY")
    (System/getenv "LEANCLOUD_APP_MASTER_KEY")))

(deftest-
  prepare-test
  (AVObject/deleteAll (query-find (avos-query "Test")))
  (AVObject/deleteAll (query-find (avos-query "TestParent"))))

(defn object-get-slot-test
  [object]
  (is (= {:number 1
          :txt    "hello world"
          :mySeq  [1 2 3]
          :map1   {"hello" "world"}}
         (get-slot object ["number" :num
                           "txt" :str
                           "mySeq" :seq
                           "map1" :map]))))

(deftest
  avos-object-test
  (let [object (avos-object "Test")]
    (is (instance? AVObject object))
    (is (put-object object ["hello" "world"]))
    (is (= "world" (.getString object "hello")))
    (is (put-object object ["number" 1]))
    (is (= 1 (.getInt object "number")))
    (is (put-object object [:flatten-object {:txt "hello world" :map1 {:hello "world"} :mySeq [1, 2, 3]}]))
    (is (= "hello world" (.getString object "txt")))
    (is (put-object object ["myObj" (-> (avos-object "Test")
                                        (put-object ["hello" "world!!!"])
                                        (save-object))]))
    (is (instance? AVObject (save-object object)))
    (object-get-slot-test object)))

(deftest
  avos-relation-test
  (let [obj1 (avos-object "Test")
        obj2 (avos-object "Test")
        obj3 (avos-object "Test")
        parent (avos-object "TestParent")]
    (put-object obj1 ["hello" "world1" "number" 2])
    (put-object obj2 ["hello" "world2" "number" 3])
    (put-object obj3 ["hello" "world3" "number" 6])
    (save-objects [obj1 obj2 obj3])
    (add-relation parent "testRealation" obj1 obj2 obj3 obj3)
    (put-object parent ["haha" "xixi"])
    (is (save-object parent))
    (let [p (query-find (relation-query parent "testRealation"))]
      (is (= 3 (count p))))))

(deftest
  avos-query-test
  (is (instance? AVQuery (query-object
                           (avos-query "Test")
                           ["number" := 1])))
  (is (= 1 (count (query-find
                    (query-object
                      (avos-query "Test")
                      ["number" := 1])))))
  (is (= 0 (count (query-find
                    (query-object
                      (avos-query "Test")
                      ["hello" := "haha"])))))
  (is (not= 0 (count (query-find
                       (query-object
                         (avos-query "Test")
                         ["hello" :not= "haha"])))))
  (is (= 1 (count (query-find
                    (query-object
                      (avos-query "Test")
                      ["number" :>= 4]))))))