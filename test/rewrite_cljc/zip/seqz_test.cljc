(ns rewrite-cljc.zip.seqz-test
  (:require [clojure.test :refer [deftest testing is]]
            [rewrite-cljc.custom-zipper.core :as z]
            [rewrite-cljc.zip.base :as base]
            [rewrite-cljc.zip.editz :as e]
            [rewrite-cljc.zip.seqz :as sq]))

(deftest t-seq
  (let [v (base/of-string "[1 2 3]")
        m (base/of-string "{:a 0, :b 1}")
        m-ns (base/of-string "#::my-ns-alias{:x 42, ::y 17}")
        e (base/of-string "{}")]
    (testing "map over vector values"
      (is (= "[2 3 4]" (base/string (sq/map #(e/edit % inc) v)))))
    (testing "map over map keys and vals"
      (is (= "{\"a\" 0, \"b\" 1}" (base/string (sq/map-keys #(e/edit % name) m))))
      (is (= "{:a 1, :b 2}" (base/string (sq/map-vals #(e/edit % inc) m)))))
    (testing "get from map and vector"
      (is (= 0 (-> m (sq/get :a) base/sexpr)))
      (is (= 1 (-> m (sq/get :b) base/sexpr)))
      (is (nil? (sq/get m :c)))
      (is (= 2 (-> v (sq/get 1) base/sexpr)))
      (is (= 42 (-> m-ns (sq/get :my-ns-alias-unresolved/x) base/sexpr)))
      (is (= 17 (-> m-ns (sq/get :user/y) base/sexpr))) )
    (testing "assoc map and vector"
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
        (is (= "[1 2 4]" (base/string v')))))))

(deftest t-sexpr-namespaced-maps
  (let [m (base/of-string "{:a 1 :b 2}")
        m-q (base/of-string "#:prefix{:a 1 :b 2}")
        m-ar-alias (base/of-string "#::my-ns-alias{:a 1 :b 2}")
        m-ar-cur-ns (base/of-string "#::{:a 1 :b 2}")
        opts {:auto-resolve (fn [ns-alias]
                              (if (= :current ns-alias)
                                'my.current.ns
                                (get {'my-ns-alias 'my.aliased.ns}
                                     ns-alias
                                     (symbol (str ns-alias "-unresolved")))))}]
    (testing "unqualified map keys are unaffected by resolvers"
      (is (= {:a 1 :b 2} (base/sexpr m)))
      (is (= {:a 1 :b 2} (base/sexpr m opts ))))
    (testing "qualified map keys are unaffected by resolvers"
      (is (= {:prefix/a 1 :prefix/b 2} (base/sexpr m-q)))
      (is (= {:prefix/a 1 :prefix/b 2} (base/sexpr m-q opts))) )
    (testing "auto-resolve ns-alias map keys are affected by resolvers"
      (is (= {:my-ns-alias-unresolved/a 1 :my-ns-alias-unresolved/b 2} (base/sexpr m-ar-alias)))
      (is (= {:my.aliased.ns/a 1 :my.aliased.ns/b 2} (base/sexpr m-ar-alias opts))) )
    (testing "auto-resolve current-ns map keys are affected by resolvers"
      (is (= {:user/a 1 :user/b 2} (base/sexpr m-ar-cur-ns)))
      (is (= {:my.current.ns/a 1 :my.current.ns/b 2} (base/sexpr m-ar-cur-ns opts))) )
    (testing "changing a map's namespaced type needs to be explicitly reflected to child keys"
      (is (= {:user/a 1 :user/b 2} (base/sexpr m-ar-cur-ns)))
      (is (= {:my.current.ns/a 1 :my.current.ns/b 2} (base/sexpr m-ar-cur-ns opts))) ) ))

(comment
  (def m (base/of-string "{:a 1 :b 2}"))
  (def n (z/node m))
  (def new-n (assoc n :prefix "my-prefix"))
  (def new-m (e/replace m new-n))

  (base/sexpr new-m)

  (-> m
      (e/edit #(assoc % :prefix "my-prefix"))
      base/sexpr)

  (base/sexpr (assoc  (z/node m) :prefix "my-prefix"))

  )

(comment
  (get #:my-prefix{:a 1} :my-prefix/a)


  (def m (base/of-string "{:a 0, :b 1}"))
  (def v (base/of-string "[1 2 3]"))
  (base/string (sq/map-keys #(e/edit % name) m))
  (-> (base/of-string "#::my-ns-alias{:x 42, ::y 17}")
      base/sexpr
      )
  (:x {:my-ns-alias-unresolved/x 22})
  (-> (sq/get m :b) base/string)
  (-> (sq/assoc m :b 122) base/string)
  (-> (sq/assoc v 3 333) base/string)
  )


(deftest t-check-predicates
  (is (-> "[1 2 3]" base/of-string sq/vector?))
  (is (-> "{:a 1}" base/of-string sq/map?))
  (is (-> "#{1 2}" base/of-string sq/set?))
  (is (-> "(+ 2 3)" base/of-string sq/list?))
  (is (-> "[1 2]" base/of-string sq/seq?)))

(deftest t-get-from-vector-index-out-of-bounds
  (is (thrown? #?(:clj IndexOutOfBoundsException :cljs js/Error)
               (-> "[5 10 15]" base/of-string (sq/get 5) z/node :value))))

(deftest t-map-on-vector-preserves-whitespace
  (let [sexpr "[ 1\n2\n3]"
        expected "[ 5\n6\n7]"]
    (is (= expected (->> sexpr base/of-string (sq/map #(e/edit % + 4)) base/root-string)))))
