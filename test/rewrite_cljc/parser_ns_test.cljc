(ns ^:skip-for-sci  ;; these creation/finding of namespaces
    ^:skip-for-cljs ;; ok for bootstrap cljs, but not regular cljs
    rewrite-cljc.parser-ns-test
  (:require [clojure.test :refer [deftest is are]]
            [clojure.tools.reader :refer [*alias-map*]]
            [rewrite-cljc.node :as node]
            [rewrite-cljc.parser :as p]))

(deftest ^:skip-for-cljs t-parsing-auto-resolves
  (are [?s ?r]
      (binding [*ns* (create-ns 'rewrite-cljc.parser-ns-test)]
        (let [n (p/parse-string ?s)]
          (is (= :token (node/tag n)))
          (is (= ?s (node/string n)))
          (is (= ?r (node/sexpr n)))))
    "::1.5.1"                    ::1.5.1
    "::key"                      ::key
    "::xyz/key"                  :xyz/key))

(deftest ^:skip-for-cljs t-parsing-auto-resolve-namespaced-maps
  (are [?s ?children ?sexpr]
      (binding [*ns* (create-ns 'rewrite-cljc.parser-ns-test)]
        (let [n (p/parse-string ?s)]
          (is (= :namespaced-map (node/tag n)))
          (is (= (count ?s) (node/length n)))
          (is (= ?s (node/string n)))
          (is (= ?children (str (node/children n))))
          (is (= ?sexpr (node/sexpr n)))))
    "#::{:x 1, :y 1}"
    "[<token: ::> <map: {:x 1, :y 1}>]"
    {::x 1, ::y 1}

    "#::   {:x 1, :y 1}"
    "[<token: ::> <whitespace: \"   \"> <map: {:x 1, :y 1}>]"
    {::x 1, ::y 1}

    "#::{:kw 1, :n/kw 2, :_/bare 3, 0 4}"
    "[<token: ::> <map: {:kw 1, :n/kw 2, :_/bare 3, 0 4}>]"
    {::kw 1, :n/kw 2, :bare 3, 0 4}

    "#::{:a {:b 1}}"
    "[<token: ::> <map: {:a {:b 1}}>]"
    {::a {:b 1}}

    "#::{:a #::{:b 1}}"
    "[<token: ::> <map: {:a #::{:b 1}}>]"
    {::a {::b 1}}))

(defn parsing-auto-resolve-alias-namespaced-maps[]
  (are [?s ?children ?sexpr]
      (let [n (p/parse-string ?s)]
        (is (= :namespaced-map (node/tag n)))
        (is (= (count ?s) (node/length n)))
        (is (= ?s (node/string n)))
        (is (= ?children (str (node/children n))))
        (is (= ?sexpr (node/sexpr n))))
    "#::node{:x 1, :y 1}"
    "[<token: ::node> <map: {:x 1, :y 1}>]"
    '{::node/x 1, ::node/y 1}

    "#::node   {:x 1, :y 1}"
    "[<token: ::node> <whitespace: \"   \"> <map: {:x 1, :y 1}>]"
    '{::node/x 1, ::node/y 1}

    "#::node{:kw 1, :n/kw 2, :_/bare 3, 0 4}"
    "[<token: ::node> <map: {:kw 1, :n/kw 2, :_/bare 3, 0 4}>]"
    '{::node/kw 1, :n/kw 2, :bare 3, 0 4}

    "#::node{:a {:b 1}}"
    "[<token: ::node> <map: {:a {:b 1}}>]"
    '{::node/a {:b 1}}

    "#::node{:a #::node{:b 1}}"
    "[<token: ::node> <map: {:a #::node{:b 1}}>]"
    '{::node/a {::node/b 1}}))


#?(:clj
   (deftest
     t-parsing-auto-resolve-alias-namespaced-maps-clj-style
     (binding [*ns* (find-ns 'rewrite-cljc.parser-ns-test)]
       (parsing-auto-resolve-alias-namespaced-maps))))

(deftest ^:skip-for-cljs
  t-parsing-auto-resolve-alias-namespaced-maps-cljs-style
  (binding [*alias-map* '{node rewrite-cljc.node}]
    (parsing-auto-resolve-alias-namespaced-maps)))
