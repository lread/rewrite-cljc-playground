(ns rewrite-cljc.zip.removez-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-cljc.zip.base :as base]
            [rewrite-cljc.zip.move :as m]
            [rewrite-cljc.zip.removez :as r]))

(deftest t-whitespace-aware-removal
  (are [?data ?n ?s]
       (let [elements (->> (base/of-string ?data)
                           (iterate m/next))
             loc (nth elements ?n)
             loc' (r/remove loc)]
         (is (= ?s (base/root-string loc'))))
    "[1 2 3 4]"    0    ""
    "[1 2 3 4]"    1    "[2 3 4]"
    "[1 2 3 4]"    2    "[1 3 4]"
    "[1 2 3 4]"    3    "[1 2 4]"
    "[1 2 3 4]"    4    "[1 2 3]"
    "[ 1 2 3 4]"   1    "[2 3 4]"
    "[1 2 3 4 ]"   4    "[1 2 3]"
    "[1]"          1    "[]"
    "[   1   ]"    1    "[]"
    "[;; c\n1]"    1    "[;; c\n]"
    "[1\n;; c\n2]" 1    "[;; c\n2]"
    "[1\n;; c\n2]" 2    "[1\n;; c\n]"))

(deftest t-more-whitespace
  (let [root (base/of-string
              (str "  :k [[a b c]\n"
                   "      [d e f]]\n"
                   "  :keyword 0"))]
    (is (= (str "  :k [[d e f]]\n"
                "  :keyword 0")
           (-> root m/next m/down r/remove base/root-string)))))

(deftest t-removing-after-comment
  (let [loc (-> (base/of-string "; comment\nx")
                (m/rightmost)
                (r/remove))]
    (is (= "; comment\n" (base/root-string loc)))))
