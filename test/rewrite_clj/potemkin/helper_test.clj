(ns rewrite-clj.potemkin.helper-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [rewrite-clj.potemkin.helper :as helper]))

(deftest t-unravel-syms
  (is (= '([one.two.three/a {}]
           [one.two.three/b {}]
           [sunday.sally/f {:name-to-pattern "@@orig-name@@*"}]
           [monday.molly/g {:name-to-pattern "@@orig-name@@*"}]
           [monday.molly/h {:name-to-pattern "@@orig-name@@*"}]
           [wednesday.willy/i {:doc-to-pattern "Orig sym @@orig-name@@ orig doc @@orig-doc@@*"}]
           [thursday.theo/j {:name-to-pattern "@@orig-name@@-new"
                              :doc-to-pattern "Sym @@orig-name@@ doc @@orig-doc@@*"}]
           [friday.fred/k {}])
         (helper/unravel-syms [['one.two.three 'a 'b]
                               {:name-to-pattern "@@orig-name@@*"}
                               ['sunday.sally 'f]
                               ['monday.molly 'g 'h]
                               {:doc-to-pattern "Orig sym @@orig-name@@ orig doc @@orig-doc@@*"}
                               ['wednesday.willy 'i]
                               {:name-to-pattern "@@orig-name@@-new"
                                :doc-to-pattern "Sym @@orig-name@@ doc @@orig-doc@@*"}
                               ['thursday.theo 'j]
                               {}
                               ['friday.fred 'k]]))))

(deftest t-new-meta-doc-changed
  (is (= {:doc "Orignal sym `original-name` and original doc `original doc`" :name 'original-name :line 234}
         (helper/new-meta
          {:name 'original-name :doc "original doc" :line 234}
          {:doc-to-pattern "Orignal sym `@@orig-name@@` and original doc `@@orig-doc@@`"}))))

(deftest t-new-meta-doc-changed-no-orig-doc
  (is (= {:doc "Orignal sym `original-name` and original doc ``" :name 'original-name :line 234}
         (helper/new-meta
          {:name 'original-name :line 234}
          {:doc-to-pattern "Orignal sym `@@orig-name@@` and original doc `@@orig-doc@@`"}))))

(deftest t-new-meta-doc-unchanged
  (is (= {:name 'original-name :doc "original doc" :line 234}
         (helper/new-meta {:name 'original-name :doc "original doc" :line 234}
                          nil))))
