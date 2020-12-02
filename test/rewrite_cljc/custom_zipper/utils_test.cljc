(ns rewrite-cljc.custom-zipper.utils-test
  (:require [clojure.test :refer [deftest testing is are]]
            [rewrite-cljc.custom-zipper.core :as z]
            [rewrite-cljc.custom-zipper.utils :as u]
            [rewrite-cljc.node :as node]
            [rewrite-cljc.zip.base :as base])
  #?(:clj (:import clojure.lang.ExceptionInfo)))

(deftest t-remove-sibling
  (let [a (node/token-node 'a)
        b (node/token-node 'b)
        c (node/token-node 'c)
        d (node/token-node 'd)
        loc (z/down (base/edn* (node/forms-node [a b c d])))]
    (testing "remove-right"
      (let [loc' (u/remove-right loc)]
        (is (= 'a (base/sexpr loc')))
        (is (= "acd" (base/root-string loc')))))
    (testing "remove-left"
      (let [loc' (-> loc z/right z/right u/remove-left)]
        (is (= 'c (base/sexpr loc')))
        (is (= "acd" (base/root-string loc')))))
    (testing "remove-and-move-right"
      (let [loc' (u/remove-and-move-right (z/right loc))]
        (is (= 'c (base/sexpr loc')))
        (is (= "acd" (base/root-string loc')))))
    (testing "remove-and-move-left"
      (let [loc' (-> loc z/right u/remove-and-move-left)]
        (is (= 'a (base/sexpr loc')))
        (is (= "acd" (base/root-string loc')))))))

(deftest t-remove-and-move-up
  (let [root (base/of-string "[a [b c d]]")]
    (are [?n ?sexpr ?root-string]
        (let [zloc (nth (iterate z/next root) ?n)
              zloc' (u/remove-and-move-up zloc)]
          (is (= ?sexpr (base/sexpr zloc')))
          (is (= ?root-string (base/root-string zloc'))))
      4 '[c d] "[a [ c d]]"
      6 '[b d] "[a [b  d]]")
    (is (thrown-with-msg? ExceptionInfo #"cannot remove at top"
                          (u/remove-and-move-up root)))))

(deftest t-remove-and-move-left-tracks-current-position-correctly
  (are [?n ?pos]
       (let [root (base/of-string "[a bb ccc]" {:track-position? true})
             zloc (nth (iterate z/next root) ?n)]
         (is (= ?pos (z/position (u/remove-and-move-left zloc)))))
    3  [1 3]
    5  [1 6]
    2  [1 2]))

(deftest t-remove-and-move-right-does-not-affect-position
  (are [?n ?pos]
       (let [root (base/of-string "[a bb ccc]" {:track-position? true})
             zloc (nth (iterate z/next root) ?n)]
         (is (= ?pos (z/position (u/remove-and-move-right zloc)))))
    3  [1 4]
    1  [1 2]
    2  [1 3]))

(deftest t-remove-left-tracks-current-position-correctly
  (are [?n ?pos]
       (let [root (base/of-string "[a bb ccc]" {:track-position? true})
             zloc (nth (iterate z/next root) ?n)]
         (is (= ?pos (z/position (u/remove-left zloc)))))
    3  [1 3]
    5  [1 6]))

(deftest t-remove-and-move-up-tracks-current-position-correctly
  (are [?n ?pos]
      (let [root (base/of-string "[a1 [bb4 ccc6]]" {:track-position? true})
            zloc (nth (iterate z/next root) ?n)]
        (is (= ?pos (z/position (u/remove-and-move-up zloc)))))
    4 [1 5]
    6 [1 5]))
