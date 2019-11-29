(ns rewrite-cljc.zip.seqz-test
  (:require [clojure.test :refer [deftest is]]
            [rewrite-cljc.zip.base :as base]
            [rewrite-cljc.zip.editz :as e]
            [rewrite-cljc.zip.seqz :as sq]
            [rewrite-cljc.custom-zipper.core :as z]))

(let [v (base/of-string "[1 2 3]")
      m (base/of-string "{:a 0, :b 1}")
      e (base/of-string "{}")]
  (deftest t-iteration
    (is (= "[2 3 4]" (base/string (sq/map #(e/edit % inc) v)))))
  (deftest t-iteration-over-map-keysvalues
    (is (= "{\"a\" 0, \"b\" 1}" (base/string (sq/map-keys #(e/edit % name) m))))
    (is (= "{:a 1, :b 2}" (base/string (sq/map-vals #(e/edit % inc) m)))))
  (deftest t-get
    (is (= 0 (-> m (sq/get :a) base/sexpr)))
    (is (= 1 (-> m (sq/get :b) base/sexpr)))
    (is (nil? (sq/get m :c)))
    (is (= 2 (-> v (sq/get 1) base/sexpr))))
  (deftest t-assoc
    (let [m' (sq/assoc m :a 3)]
      (is (= {:a 3 :b 1} (base/sexpr m')))
      (is (= "{:a 3, :b 1}" (base/string m'))))
    (let [m' (sq/assoc m :c 2)]
      (is (= {:a 0 :b 1, :c 2} (base/sexpr m')))
      (is (= "{:a 0, :b 1 :c 2}" (base/string m'))))
    (let [m' (sq/assoc e :x 0)]
      (is (= {:x 0} (base/sexpr m')))
      (is (= "{:x 0}" (base/string m'))))
    (let [v' (sq/assoc v 2 4)]
      (is (= [1 2 4] (base/sexpr v')))
      (is (= "[1 2 4]" (base/string v'))))))

(deftest t-check-predicates
  (is (-> "[1 2 3]" base/of-string sq/vector?))
  (is (-> "{:a 1}" base/of-string sq/map?))
  (is (-> "#{1 2}" base/of-string sq/set?))
  (is (-> "(+ 2 3)" base/of-string sq/list?))
  (is (-> "[1 2]" base/of-string sq/seq?)))

(deftest t-get-from-vector-index-out-of-bounds
  (is (thrown? #?(:clj IndexOutOfBoundsException :cljs js/Error)
               (-> "[5 10 15]" base/of-string (sq/get 5) z/node :value))))

(deftest t-map-on-vector
  (let [sexpr "[1\n2\n3]"
        expected "[5\n6\n7]"]
    (is (= expected (->> sexpr base/of-string (sq/map #(e/edit % + 4)) base/root-string)))))
