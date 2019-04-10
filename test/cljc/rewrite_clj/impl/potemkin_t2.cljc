(ns rewrite-clj.impl.potemkin-t2)

(def d 42)

(def dd "def with doc" 77)

(defn f [arg] arg)

(defn fd "function with doc" [arg] arg)

(defmacro m [a] a)

(defmacro md "macro with doc" [a b c d] '(str a b c d))
