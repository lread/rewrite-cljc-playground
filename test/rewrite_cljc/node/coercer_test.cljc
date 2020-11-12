(ns rewrite-cljc.node.coercer-test
  (:require [clojure.test :refer [deftest is are]]
            [rewrite-cljc.node.protocols :as node :refer [coerce]]
            [rewrite-cljc.parser :as p]))

(deftest t-sexpr->node->sexpr-roundtrip
  (are [?sexpr expected-tag]
      (let [n (coerce ?sexpr)]
        (is (node/node? n))
        (is (= expected-tag (node/tag n)))
        (is (string? (node/string n)))
        (is (= ?sexpr (node/sexpr n)))
        (is (not (meta n)))
        (is (= (type ?sexpr) (type (node/sexpr n)))))
    ;; numbers
    3                      :token
    3N                     :token
    3.14                   :token
    3.14M                  :token
    3e14                   :token

    ;; ratios are not valid in cljs
    #?@(:clj  [3/4         :token])

    ;; symbol/keyword/string/...
    'symbol                :token
    'namespace/symbol      :token
    :keyword               :token
    :1.5.1                 :token
    ::keyword              :token
    ::1.5.1                :token
    :namespace/keyword     :token
    ""                     :token
    "hello, over there!"   :token
    "multi\nline"          :token

    ;; seqs
    []                     :vector
    [1 2 3]                :vector
    ()                     :list
    '()                    :list
    (list 1 2 3)           :list
    #{}                    :set
    #{1 2 3}               :set

    ;; date
    #inst "2014-11-26T00:05:23" :token))

(deftest
  t-quoted-list-reader-location-metadata-elided
  (are [?sexpr expected-meta-keys]
      (let [n (coerce ?sexpr)]
        (is (node/node? n))
        (is (= expected-meta-keys (node/string n)))
        (is (string? (node/string n)))
        (is (= ?sexpr (node/sexpr n)))
        (is (not (meta n)))
        (is (= (type ?sexpr) (type (node/sexpr n)))))
    '(1 2 3) "(1 2 3)"
    '^:other-meta (4 5 6) "^{:other-meta true} (4 5 6)"))

(deftest
  ^:skip-for-sci
  t-quoted-list-reader-location-metadata-included-on-request
  (binding [node/*elide-metadata* nil]
    (are [?sexpr expected-meta-keys]
        (let [n (coerce ?sexpr)]
          (is (node/node? n))
          (is (= expected-meta-keys (when (= :meta (node/tag n))
                                      (set (keys (node/sexpr (first (node/children n))))))))
          (is (string? (node/string n)))
          (is (= ?sexpr (node/sexpr n)))
          (is (not (meta n)))
          (is (= (type ?sexpr) (type (node/sexpr n)))))
      '(1 2 3) #?(:clj #{:line :column }
                  ;; cljs does not add location metadata
                  :cljs nil)
      '^:other-meta (4 5 6) #?(:clj #{:line :column :other-meta}
                               :cljs #{:other-meta}))))

(deftest t-maps
  (are [?sexpr]
      (let [n (coerce ?sexpr)]
        (is (node/node? n))
        (is (= :map (node/tag n)))
        (is (string? (node/string n)))
        (is (= ?sexpr (node/sexpr n)))
        ;; we do not restore to original map (hash-map or array-map),
        ;; checking if we convert to any map is sufficient
        (is (map? (node/sexpr n))))
    {}
    {:a 1 :b 2}
    (hash-map)
    (hash-map :a 0 :b 1)
    (array-map)
    (array-map :d 4 :e 5)))

(deftest t-sexpr->node->sexpr-roundtrip-for-regex
  (let [sexpr #"abc"
        n (coerce sexpr)]
    (is (node/node? n))
    (is (string? (node/string n)))
    (is (= (str sexpr) (str (node/sexpr n))))
    (is (= (type sexpr) (type (node/sexpr n))))))

(deftest
  ^:skip-for-sci ;; sci, by design has its own var type, so skip this one for sci
  t-vars
  (let [n (coerce #'identity)]
    (is (node/node? n))
    (is (= '(var #?(:clj clojure.core/identity :cljs cljs.core/identity)) (node/sexpr n)))))

(deftest t-nil
  (let [n (coerce nil)]
    (is (node/node? n))
    (is (= nil (node/sexpr n)))
    (is (= n (p/parse-string "nil")))))

(defrecord Foo-Bar [a])

(deftest
  ^:skip-for-sci ;; records have metadata in sci, so skip this one for sci
  t-records
  (let [v (Foo-Bar. 0)
        n (coerce v)]
    (is (node/node? n))
    ;; TODO: why is a record tagged as a :reader-macro?
    (is (= :reader-macro (node/tag n)))
    (is (= (pr-str v) (node/string n)))))
