(ns rewrite-cljc.zip-test
  "This test namespace originated from rewrite-cljs."
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is]]
            [rewrite-cljc.zip :as z]))

(deftest of-string-simple-sexpr
  (let [sexpr "(+ 1 2)"]
   (is (= sexpr (-> sexpr z/of-string z/root-string)))))

(deftest manipulate-sexpr
  (let [sexpr
        (string/join
         "\n" [""
               " ^{:dynamic true} (+ 1 1"
               "   (+ 2 2)"
               "   (reduce + [1 3 4]))"])
        expected
        (string/join
         "\n" [""
               " ^{:dynamic true} (+ 1 1"
               "   (+ 2 2)"
               "   (reduce + [6 7 [1 2]]))"])]
    (is (= expected (-> (z/of-string sexpr {:track-position? true})
                        (z/find-tag-by-pos {:row 4 :col 19} :vector) ;; should find [1 3 4] col 19 points to element 4 in vector
                        (z/replace [5 6 7])                          ;; replaces [1 3 4] with [5 6 7]
                        (z/append-child [1 2])                       ;; appends [1 2] to [5 6 7] giving [5 6 [1 2]]
                        z/down                                       ;; navigate to 5
                        z/remove                                     ;; remove 5 giving [6 7 [1 2]]
                        z/root-string)))))

(deftest namespaced-keywords
  (is (= ":dill" (-> ":dill" z/of-string z/root-string)))
  (is (= "::dill" (-> "::dill" z/of-string z/root-string)))
  (is (= ":dill/dall" (-> ":dill/dall" z/of-string z/root-string)))
  (is (= "::dill/dall" (-> "::dill/dall" z/of-string z/root-string)))
  (is (= ":%dill.*" (-> ":%dill.*" z/of-string z/root-string))))
