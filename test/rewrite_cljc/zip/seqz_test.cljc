(ns rewrite-cljc.zip.seqz-test
  (:require [clojure.test :refer [deftest testing is are]]
            [rewrite-cljc.zip.base :as base]
            [rewrite-cljc.zip.editz :as e]
            [rewrite-cljc.zip.seqz :as sq]))

(deftest t-predicates
  (are [?s ?p]
      (do
        (is (-> ?s base/of-string sq/seq?))
        (is (-> ?s base/of-string ?p)))
    "[1 2 3]" sq/vector?
    "{:a 1}" sq/map?
    "#:my-prefix {:a 1}" sq/namespaced-map?
    "(+ 2 3)" sq/list?
    "#{1 2}" sq/set?))

(deftest t-seq
  (testing "map and map-vals are equivalent and update values in maps"
    (are [?sin ?sout]
        (do
          (is (= ?sout (->> ?sin
                            base/of-string
                            (sq/map-vals #(e/edit % inc))
                            base/string)))
          (is (= ?sout (->> ?sin
                            base/of-string
                            (sq/map #(e/edit % inc))
                            base/string))))
      "{:a 0, :b 1}"
      "{:a 1, :b 2}"

      "#::my-ns-alias{:x 42, ::y 17}"
      "#::my-ns-alias{:x 43, ::y 18}") )

  (testing "map-keys works for maps"
    (are [?sin ?sout]
        (is (= ?sout (->> ?sin
                          base/of-string
                          ;; TODO: This is a little misleading.. edit works on sexpr of keyword
                          ;; and namespaced map keyword sexpr will be affected
                          (sq/map-keys #(e/edit % name))
                          base/string)))
      "{:a 0, :b 1}"
      "{\"a\" 0, \"b\" 1}"

      "#:my-prefix {:x 7, :y 123}"
      "#:my-prefix {\"x\" 7, \"y\" 123}"))
  (testing "map works for seqs and preserve whitespace"
    (is (= "[ 5\n6\n7]" (->> "[ 1\n2\n3]"
                             base/of-string
                             (sq/map #(e/edit % + 4))
                             base/string))))
  (testing "get on maps and vectors"
    (are [?sin ?key ?expected-value]
        (is (= ?expected-value (-> ?sin
                                   base/of-string
                                   (sq/get ?key)
                                   base/sexpr)))
      "{:a 0 :b 1}" :a 0
      "{:a 3, :b 4}" :b 4
      "{:x 10 :y 20}" :z nil
      "[1 2 3]" 1 2
      ;; TODO: is this usable? having to specify the resolved prefix?
      "#:my-prefix{:c 10 :d 11}" :my-prefix/d 11
      "#::my-ns-alias{:x 42, ::y 17}" :my-ns-alias-unresolved/x 42
      "#::my-ns-alias{:x 42, ::y 17}" :user/y 17))

  (testing "assoc map and vector"
    (are [?sin ?key ?value ?sout]
        (is (= ?sout (-> ?sin
                         base/of-string
                         (sq/assoc ?key ?value)
                         base/string)))
      "{:a 0, :b 1}" :a 3 "{:a 3, :b 1}"
      "{:a 0, :b 1}" :c 2 "{:a 0, :b 1 :c 2}"
      "{}" :x 0 "{:x 0}"
      "[1 2 3]" 2 703 "[1 2 703]"
      ;; TODO: review namespaced maps and keywords.
      "#:my-prefix{:c 10 :d 11}" :my-prefix/d "new-d-val" "#:my-prefix{:c 10 :d \"new-d-val\"}"
      "#::my-ns-alias{:x 42, ::y 17}" :my-ns-alias-unresolved/x "new-x-val" "#::my-ns-alias{:x \"new-x-val\", ::y 17}"))
  (testing "out of bounds assoc on vector should throw"
    (is (thrown? #?(:clj IndexOutOfBoundsException :cljs js/Error)
                 (-> "[5 10 15]" base/of-string (sq/get 5))))))

;; TODO: These aren't seq operation tests
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
      (is (= {:a 1 :b 2} (base/sexpr m opts))))
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
      (is (= {:my.current.ns/a 1 :my.current.ns/b 2} (base/sexpr m-ar-cur-ns opts))))))
