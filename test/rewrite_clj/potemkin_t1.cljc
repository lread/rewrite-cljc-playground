(ns rewrite-clj.potemkin-t1
  #?(:cljs (:require-macros [rewrite-clj.potemkin-t1])))

(def t-def 42)

(def t-def-doc "def with doc" 77)

(defn t-fn [arg] arg)

(defn t-fn-doc "function with doc" [arg] arg)

(defmacro t-macro [a] a)

(defmacro t-macro-doc "macro with doc" [a b c d] `(str ~a ~b ~c ~d))
