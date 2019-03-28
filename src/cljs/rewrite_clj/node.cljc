(ns rewrite-clj.node
  "API to create and evaluate zipper tree nodes."
  (:refer-clojure :exclude [string coerce])
  (:require [rewrite-clj.impl.node.coerce]
            [rewrite-clj.impl.node.comment]
            [rewrite-clj.impl.node.fn]
            [rewrite-clj.impl.node.forms]
            [rewrite-clj.impl.node.integer]
            [rewrite-clj.impl.node.keyword]
            [rewrite-clj.impl.node.meta]
            [rewrite-clj.impl.node.protocols]
            [rewrite-clj.impl.node.quote]
            [rewrite-clj.impl.node.reader-macro]
            [rewrite-clj.impl.node.regex]
            [rewrite-clj.impl.node.seq]
            [rewrite-clj.impl.node.string]
            [rewrite-clj.impl.node.token]
            [rewrite-clj.impl.node.uneval]
            [rewrite-clj.impl.node.whitespace]
            #?(:clj [rewrite-clj.impl.potemkin :refer [import-vars]]))
  #?(:cljs (:require-macros [rewrite-clj.impl.potemkin2-cljs :refer [import-vars]])))

(import-vars
 [rewrite-clj.impl.node.protocols
  coerce
  children
  child-sexprs
  concat-strings
  inner?
  leader-length
  length
  printable-only?
  replace-children
  sexpr
  sexprs
  string
  tag]

 [rewrite-clj.impl.node.comment
  comment-node
  comment?]

 [rewrite-clj.impl.node.fn
  fn-node]

 [rewrite-clj.impl.node.forms
  forms-node]

 [rewrite-clj.impl.node.integer
  integer-node]

 [rewrite-clj.impl.node.keyword
  keyword-node]

 [rewrite-clj.impl.node.meta
  meta-node
  raw-meta-node]

 [rewrite-clj.impl.node.regex
  regex-node]

 [rewrite-clj.impl.node.reader-macro
  deref-node
  eval-node
  reader-macro-node
  var-node]

 [rewrite-clj.impl.node.seq
  list-node
  map-node
  namespaced-map-node
  set-node
  vector-node]

 [rewrite-clj.impl.node.string
  string-node]

 [rewrite-clj.impl.node.quote
  quote-node
  syntax-quote-node
  unquote-node
  unquote-splicing-node]

 [rewrite-clj.impl.node.token
  token-node]

 [rewrite-clj.impl.node.uneval
  uneval-node]

 [rewrite-clj.impl.node.whitespace
  comma-separated
  line-separated
  linebreak?
  newlines
  newline-node
  spaces
  whitespace-node
  whitespace?
  comma-node
  comma?
  whitespace-nodes])

;; ## Predicates

(defn whitespace-or-comment?
  "Check whether the given node represents whitespace or comment."
  [node]
  (or (whitespace? node)
      (comment? node)))

;; ## Value

(defn ^{:deprecated "0.4.0"} value
  "DEPRECATED: Get first child as a pair of tag/sexpr (if inner node),
   or just the node's own sexpr. (use explicit analysis of `children`
   `child-sexprs` instead) "
  [node]
  (if (inner? node)
    (some-> (children node)
            (first)
            ((juxt tag sexpr)))
    (sexpr node)))
